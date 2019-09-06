import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import '../node/node_modules/style-loader!../node/node_modules/vis/dist/vis.css';
import {Network} from '../node/node_modules/vis/dist/vis';
import {Link} from '../node/node_modules/react-router-dom';
import DataE2e from './DataE2e.js';
import Groups from './Groups.js';
import options from './options.js';

class DefE2e extends Component {
    
  constructor(...args) {
    super(...args);
    this.state = {
      activity: {}, 
      data: {},
      assetPath: location.hash.substring(29)
    };
  }  

  drawGraph() {
    const container = document.getElementById('milestone-e2e-graph');
    if (container) {
      const graphOptions = Object.assign({}, { 
        height: (this.state.data.maxDepth * 100) + 'px' 
      }, options.graph);
      const graphData = {
        nodes: this.state.data.items, 
        edges: this.state.data.edges,
        assetPath: this.state.assetPath
      };
      const network = new Network(container, graphData, graphOptions);
      // adjust top margin for large depth
      const canvas = container.querySelector('div > canvas');
      if (canvas) {
        var top = '0px';
        if (this.state.data.maxDepth) {
          top = '-' + Math.round(this.state.data.maxDepth * 4.3) + 'px';
        }    
        canvas.style.top = top;
      }
      network.on('doubleClick', params => {
        if (params.nodes && params.nodes.length === 1) {
          let node = graphData.nodes[params.nodes[0]];
          if (node.packageName && node.processName) {
            let path = node.packageName + '/' + node.processName;
            if (node.processVersion) {
              path += '/' + node.processVersion;
            }
            if (node.activityId) {
              sessionStorage.setItem('mdw-activity', node.activityId);
            }
            location = this.context.hubRoot + '/#/workflow/definitions/' + path; 
          }
        }
      });
    }
  }

  componentDidMount() {
    new Groups(this.context.serviceRoot).getGroups()
    .then(groups => {
      // retrieve definition activities e2e
      const url = this.context.serviceRoot + '/Activities/definitions/e2e/' + this.state.assetPath;
      fetch(new Request(url, {
        method: 'GET',
        headers: { Accept: 'application/json', 'mdw-app-id': 'mdw-hub'},
        credentials: 'same-origin'
      }))
      .then(response => {
        return response.json();
      })
      .then(activities => {
        this.setState({
          activity: activities.activity,
          data: new DataE2e(groups, activities)
        }, this.drawGraph());
      });  
    });
  }

  render() {
    this.drawGraph();
    const slash = this.state.assetPath.lastIndexOf('/');
    const pkg = this.state.assetPath.substring(0, slash);
    const proc = this.state.assetPath.substring(slash + 1);
    const hubRoot = this.context.hubRoot;
    return (
      <div>
        <div className="panel-heading mdw-heading" style={{borderColor:'#ddd'}}>
          <div className="mdw-heading-label">
            <Link to={hubRoot + '/#/packages/' + pkg}>
              {pkg}
            </Link>
            {' / '}
            <Link to={hubRoot + '/#/workflow/definitions/' + this.state.assetPath}>
              {proc}
            </Link>
          </div>
        </div>
        <div className="mdw-section">
          <div id="milestone-e2e-graph">
          </div>
        </div>
      </div>
    );
  }
}

DefE2e.contextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string
};
export default DefE2e; 
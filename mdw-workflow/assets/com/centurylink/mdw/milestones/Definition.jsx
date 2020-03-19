import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import '../node/node_modules/style-loader!../node/node_modules/vis/dist/vis.css';
import {Network} from '../node/node_modules/vis/dist/vis';
import {Link} from '../node/node_modules/react-router-dom';
import Data from './Data.js';
import Groups from './Groups.js';
import options from './options.js';

class Definition extends Component {
    
  constructor(...args) {
    super(...args);
    this.state = {
      milestone: {}, 
      data: {},
      assetPath: location.hash.substring(25)
    };
  }  

  drawGraph() {
    const container = document.getElementById('milestone-def-graph');
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
      network.on('doubleClick', params => {
        if (params.nodes && params.nodes.length === 1) {
          let node = graphData.nodes[params.nodes[0]];
          if (node.process) {
            let path = node.process.packageName + '/' + node.process.name;
            if (node.process.version) {
              path += '/' + node.process.version;
            }
            if (node.activity) {
              sessionStorage.setItem('mdw-activity', node.activity.id);
            }
            location = this.context.hubRoot + '/#/workflow/definitions/' + path;
            // without reload the process version is cached since only url fragment changes
            location.reload();
          }
        }
      });
    }
  }

  componentDidMount() {
    new Groups(this.context.serviceRoot).getGroups()
    .then(groups => {
      // retrieve milestones definition
      const url = this.context.serviceRoot + '/com/centurylink/mdw/milestones/definitions/' + 
          this.state.assetPath;
      fetch(new Request(url, {
        method: 'GET',
        headers: { Accept: 'application/json', 'mdw-app-id': 'mdw-hub' },
        credentials: 'same-origin'
      }))
      .then(response => {
        return response.json();
      })
      .then(milestone => {
        this.setState({
          milestone: milestone.milestone,
          data: new Data(groups, milestone)
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
          <div id="milestone-def-graph">
          </div>
        </div>
      </div>
    );  
  }
}

Definition.contextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string
};
export default Definition; 
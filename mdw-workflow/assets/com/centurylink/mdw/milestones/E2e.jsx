import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import {Link} from '../node/node_modules/react-router-dom';
import '../node/node_modules/style-loader!../node/node_modules/vis/dist/vis.css';
import {Network} from '../node/node_modules/vis/dist/vis';
import HeaderPopButton from '../react/HeaderPopButton.jsx';
import DataE2e from './DataE2e.js';
import InfoPop from './InfoPop.jsx';
import Groups from './Groups.js';
import options from './options.js';

class E2e extends Component {
    
  constructor(...args) {
    super(...args);
    this.state = {
      activityInstance: {}, 
      data: {}
    };
  }  

  drawGraph() {
    const container = document.getElementById('milestone-all-graph');
    if (container) {
      $mdwUi.hubLoading(true);
      const graphOptions = Object.assign({}, { 
        height: (this.state.data.maxDepth * 100) + 'px'
      }, options.graph);
      const graphData = {
        nodes: this.state.data.items, 
        edges: this.state.data.edges
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
          if (node.processInstanceId) {
            if (node.activityInstanceId) {
              sessionStorage.setItem('mdw-activityInstance', node.activityInstanceId);
            }
            location = this.context.hubRoot + '/#/workflow/processes/' + node.processInstanceId;
          }
        }
      });
      $mdwUi.hubLoading(false);
    }
  }

  componentDidMount() {
    const masterRequestId = this.props.match.params.masterRequestId;
    new Groups(this.context.serviceRoot).getGroups()
    .then(groups => {
      // retrieve activities e2e
      const url = this.context.serviceRoot + '/Activities/e2e/' + masterRequestId;
      $mdwUi.hubLoading(true);
      var ok = false;
      fetch(new Request(url, {
        method: 'GET',
        headers: { Accept: 'application/json', NoPersistence: 'true'},
        credentials: 'same-origin'
      }))
      .then(response => {
        ok = response.ok;
        $mdwUi.hubLoading(false);
        return response.json();
      })
      .then(activities => {
        if (ok) {
          this.setState({
            activityInstance: activities.activityInstance,
            data: new DataE2e(groups, activities)
          }, this.drawGraph());
        }
        else {
          $mdwUi.showMessage(activities.status.message);
        }
      });  
    });
  }

  render() {
    this.drawGraph();
    const hubRoot = this.context.hubRoot;
    const masterRequestId = this.props.match.params.masterRequestId;

    return (
      <div>
        <div className="panel-heading mdw-heading" style={{borderColor:'#ddd'}}>
          <div className="mdw-heading-label">
            {'Traversed: '}
            <Link
              to={hubRoot + '/milestones/' + masterRequestId}>
              {masterRequestId}
            </Link>
          </div>
          <div style={{float:'right'}}>
            <HeaderPopButton label="Info" glyph="info-sign"
              popover={
                <InfoPop groups={this.state.data.groups} />
              } />
          </div>
        </div>
        <div className="mdw-section">
          <div id="milestone-all-graph">
          </div>
        </div>
      </div>
    );  
  }
}

E2e.contextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string
};
export default E2e; 
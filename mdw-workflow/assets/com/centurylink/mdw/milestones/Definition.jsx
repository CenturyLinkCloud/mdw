import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import '../node/node_modules/style-loader!../node/node_modules/vis/dist/vis.css';
import {Network} from '../node/node_modules/vis/dist/vis';
import {Link} from '../node/node_modules/react-router-dom';
import Data from './Data.js';
import options from './options.js';

class Definition extends Component {
    
  constructor(...args) {
    super(...args);
    this.state = { milestone: {}, data: {} };
  }  

  drawGraph() {
    const container = document.getElementById('definition-graph');
    if (container) {
      const graphOptions = Object.assign({}, {height: (this.state.data.maxDepth * 100) + 'px'}, options.graph);
      const graphData = {
        nodes: this.state.data.items, 
        edges: this.state.data.edges
      };
      const network = new Network(container, graphData, graphOptions);
    }
  }

  componentDidMount() {
    const processId = this.props.match.params.processId;
    const url = this.context.serviceRoot + '/com/centurylink/mdw/milestones/definitions/' + processId;
    fetch(new Request(url, {
      method: 'GET',
      headers: { Accept: 'application/json'},
      credentials: 'same-origin'
    }))
    .then(response => {
      return response.json();
    })
    .then(milestone => {
      this.setState({
        milestone: milestone.milestone,
        data: new Data(milestone)
      }, this.drawGraph());
    });
  }

  render() {
    console.log("DATA: " + JSON.stringify(this.state.data, null, 2));
    this.drawGraph();
    return (
      <div>
        <div className="panel-heading mdw-heading">
          {this.props.milestone.process &&
            <div className="mdw-heading-label">
              {this.props.milestone.process.packageName} /
              <Link className="mdw-id"
                to={this.context.hubRoot + '/todo'}>
                {this.props.milestone.process.name}
              </Link>
            </div>
          }
        </div>
        <div className="mdw-section">
          <div id="definition-graph">
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
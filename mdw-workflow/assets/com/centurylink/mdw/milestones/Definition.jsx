import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import '../node/node_modules/style-loader!../node/node_modules/vis/dist/vis.css';
import {Network} from '../node/node_modules/vis/dist/vis';
import Heading from './Heading.jsx';
import Data from './Data.js';
import options from './options.js';

class Definition extends Component {
    
  constructor(...args) {
    super(...args);
    this.state = { milestone: {}, data: {} };
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
      });
    });
  }

  render() {
    const container = document.getElementById('definition-graph');
    if (container) {
      const graphOptions = Object.assign({}, {height: (this.state.data.maxDepth * 100) + 'px'}, options.graph);
      const graphData = {
        nodes: this.state.data.items, 
        edges: this.state.data.edges
      };
      const network = new Network(container, graphData, graphOptions);
    }

    return (
      <div>
        <Heading milestone={this.state.milestone} />
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
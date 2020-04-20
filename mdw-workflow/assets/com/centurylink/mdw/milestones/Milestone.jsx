import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import {Glyphicon} from '../node/node_modules/react-bootstrap';
import {Link} from '../node/node_modules/react-router-dom';
import {Network} from '../node/node_modules/vis';
import '../node/node_modules/style-loader!../node/node_modules/vis/dist/vis.css';
import ButtonLink from '../react/ButtonLink.jsx';
import HeaderPopButton from '../react/HeaderPopButton.jsx';
import Definition from './Definition.jsx';
import DefE2e from './DefE2e.jsx';
import InfoPop from './InfoPop.jsx';
import options from './options.js';

class Milestone extends Component {

  constructor(...args) {
    super(...args);
  }

  isDef() {
    return location.hash.startsWith('#/milestones/definitions');
  }
  isDefE2e() {
    return location.hash.startsWith('#/milestones/definitions/e2e/');
  }

  drawGraph() {
    const container = document.getElementById('milestone-graph');
    if (container) {
      const graphOptions = Object.assign({}, {
        height: (this.props.data.maxDepth * 100) + 'px'
      }, options.graph);
      const graphData = {
        nodes: this.props.data.items,
        edges: this.props.data.edges
      };
      const network = new Network(container, graphData, graphOptions);
      network.on('doubleClick', params => {
        if (params.nodes && params.nodes.length === 1) {
          let node = graphData.nodes[params.nodes[0]];
          if (node.processInstance) {
            if (node.activityInstance) {
              sessionStorage.setItem('mdw-activityInstance', node.activityInstance.id);
            }
            location = this.context.hubRoot + '/#/workflow/processes/' + node.processInstance.id;
          }
        }
      });
    }
  }

  componentDidMount() {
    if (!this.isDef()) {
      this.drawGraph();
    }
  }

  render() {
    if (this.isDef()) {
      if (this.isDefE2e()) {
        return (
          <DefE2e />
        );
      }
      else {
        return (
          <Definition />
        );
      }
    }
    else {
      this.drawGraph();
      const milestone = this.props.milestone;
      const process = milestone.process;
      const hubRoot = this.context.hubRoot;
      return (
        <div>
          <div className="panel-heading mdw-heading" style={{borderColor:'#ddd'}}>
            <div className="mdw-heading-label">
              {'Milestones: '}
              <Link
                to={hubRoot + '/milestones/' + milestone.masterRequestId}>
                {milestone.label}
              </Link>
            </div>
            {process &&
              <div style={{float:'right'}}>
                <ButtonLink style={{padding:'4px 8px',fontWeight:'normal',textDecoration:'none'}}
                  to={hubRoot + '/#/milestones/definitions/' + process.packageName + '/' + process.name}>
                  <Glyphicon glyph="pencil" />
                  {' Edit'}
                </ButtonLink>
                <HeaderPopButton label="Info" glyph="info-sign"
                  popover={
                    <InfoPop groups={this.props.data.groups} />
                  } />
              </div>
            }
          </div>
          <div className="mdw-section">
            <div id="milestone-graph">
            </div>
          </div>
        </div>
      );
    }
  }
}

Milestone.contextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string
};
export default Milestone;
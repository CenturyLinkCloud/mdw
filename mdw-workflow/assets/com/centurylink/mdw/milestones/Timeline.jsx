import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import {Link} from '../node/node_modules/react-router-dom';
import {Timeline as VisTimeline, DataSet} from '../node/node_modules/vis';
import '../node/node_modules/style-loader!./milestones.css';

class Timeline extends Component {

  constructor(...args) {
    super(...args);
  }

  drawTimeline() {
    const container = document.getElementById('milestone-timeline');
    if (container) {
      const timelineData = new DataSet(this.props.data.items.filter(item => item.start));
      const timelineOptions = {
        zoomable: false
      };
      const timeline = new VisTimeline(container, timelineData, timelineOptions);
      timeline.on('doubleClick', params => {
        if (params.item) {
          let item = this.props.data.items[params.item];
          if (item.processInstance) {
            if (item.activityInstance) {
              sessionStorage.setItem('mdw-activityInstance', item.activityInstance.id);
            }
            location = this.context.hubRoot + '/#/workflow/processes/' + item.processInstance.id;
          }
        }
      });
    }
  }

  componentDidMount() {
    this.drawTimeline();
  }

  render() {
    this.drawTimeline();
    const milestone = this.props.milestone;
    return (
      <div>
        <div className="panel-heading mdw-heading">
          <div className="mdw-heading-label">
            {'Timeline: '}
            <Link
              to={this.context.hubRoot + '/milestones/' + milestone.masterRequestId}>
              {milestone.label}
            </Link>
          </div>
        </div>
        <div className="mdw-section">
          <div id="milestone-timeline">
          </div>
        </div>
      </div>
    );
  }
}

Timeline.contextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string
};
export default Timeline;
import React, {Component} from '../node/node_modules/react/react';
import PropTypes from '../node/node_modules/prop-types';
import PanelHeader from '../react/PanelHeader.jsx';
import HeaderLabel from '../react/HeaderLabel.jsx';
import HeaderDropdown from '../react/HeaderDropdown.jsx';

class RequestPath extends Component {

  constructor(...args) {
    super(...args);
    this.state = {
      paths: [],
      sample: 'Week',
      path: ''
    };
    this.handlePathSelect = this.handlePathSelect.bind(this);    
  }

  componentDidMount() {
    $mdwUi.hubLoading(true);
    fetch(new Request(this.context.serviceRoot + '/Workflow', {
      method: 'GET',
      headers: { Accept: 'application/json'},
      credentials: 'same-origin'
    }))
    .then(response => {
      return response.json();
    })
    .then(data => {
      this.setState({
        paths: data.packages,
        sample: this.state.sample,
        path: this.state.path
      });
      $mdwUi.hubLoading(false);
    });
  }

  handlePathSelect(path) {
    this.setState({
      paths: this.state.paths,
      sample: this.state.sample,
      path: path
    });
  }

  handleSampleSelect(sampleSize) {
    this.setState({
      paths: this.state.paths,
      sample: sampleSize,
      path: this.state.path
    });
  }

  render() {
    return (
      <div>
        <PanelHeader>
          <HeaderLabel title="Path:"/>
          <HeaderDropdown id="path-dropdown"
            items={['One','Two','Three']}
            selected={this.state.path}
            onSelect={this.handlePathSelect} />

          <HeaderLabel title="Sample:" style={{marginLeft:'10px'}}/>
          <HeaderDropdown id="sample-dropdown" width={100}
            items={['Day','Week','Month']}
            selected={this.state.sample}
            onSelect={this.handleSampleSelect} />
        </PanelHeader>
        <div className="mdw-section">
          Chart goes here.
        </div>
      </div>
    );
  }
}

RequestPath.contextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string
};

export default RequestPath;

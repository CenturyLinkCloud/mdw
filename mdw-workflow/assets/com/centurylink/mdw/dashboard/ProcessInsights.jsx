import React, {Component} from '../node/node_modules/react/react';
import PropTypes from '../node/node_modules/prop-types';
import PanelHeader from '../react/PanelHeader.jsx';
import HeaderLabel from '../react/HeaderLabel.jsx';
import AssetDropdown from '../react/AssetDropdown.jsx';
import HeaderDropdown from '../react/HeaderDropdown.jsx';

class ProcessInsights extends Component {

  constructor(...args) {
    super(...args);
    this.state = {
      packages: [],
      sample: 'Week',
      process: ''
    };
    this.handleProcessSelect = this.handleProcessSelect.bind(this);    
    this.handleSampleSelect = this.handleSampleSelect.bind(this);    
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
        packages: data.packages,
        sample: this.state.sample,
        process: this.state.process
      });
      $mdwUi.hubLoading(false);
    });
  }

  handleProcessSelect(assetPath) {
    this.setState({
      packages: this.state.packages,
      sample: this.state.sample,
      process: assetPath
    });
  }

  handleSampleSelect(sampleSize) {
    this.setState({
      packages: this.state.packages,
      sample: sampleSize,
      process: this.state.process
    });
  }

  render() {
    return (
      <div>
        <PanelHeader>
          <HeaderLabel title="Process:"/>
          <AssetDropdown id="process-dropdown"
            packages={this.state.packages}
            selected={this.state.process}
            onSelect={this.handleProcessSelect} />
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

ProcessInsights.contextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string
};

export default ProcessInsights;

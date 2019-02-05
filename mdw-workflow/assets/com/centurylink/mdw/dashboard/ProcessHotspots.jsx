import React, {Component} from '../node/node_modules/react/react';
import PropTypes from '../node/node_modules/prop-types';
import PanelHeader from '../react/PanelHeader.jsx';
import HeaderLabel from '../react/HeaderLabel.jsx';
import AssetDropdown from '../react/AssetDropdown.jsx';
import HeaderDropdown from '../react/HeaderDropdown.jsx';
import Workflow from '../react/Workflow.jsx';

class ProcessHotspots extends Component {

  constructor(...args) {
    super(...args);
    this.state = {
      packages: [],
      sample: 'Week',
      processDef: {}
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
        processDef: this.state.processDef,
        data: {}
      });
      $mdwUi.hubLoading(false);
    });
  }

  handleProcessSelect(assetPath) {
    $mdwUi.hubLoading(true);
    fetch(new Request(this.context.serviceRoot + '/Workflow/' + assetPath, {
      method: 'GET',
      headers: { Accept: 'application/json'},
      credentials: 'same-origin'
    }))
    .then(response => {
      return response.json();
    })
    .then(data => {
      this.setState({
        packages: this.state.packages,
        sample: this.state.sample,
        processDef: data,
        data: {}
      }, () => {
        $mdwUi.hubLoading(false);
        this.retrieveData()
        .then(data => {
          this.setState({
            packages: this.state.packages,
            sample: this.state.sample,
            processDef: this.state.processDef,
            data: data
          });  
        });
      });
    });
  }

  handleSampleSelect(sampleSize) {
    this.setState({
      packages: this.state.packages,
      sample: sampleSize,
      processDef: this.state.processDef,
      data: {}
    }, () => {
      this.retrieveData()
      .then(data => {
        this.setState({
          packages: this.state.packages,
          sample: sampleSize,
          processDef: this.state.processDef,
          data: data
        });
      });
    });
  }

  retrieveData() {
    return new Promise(resolve => {
      $mdwUi.hubLoading(true);
      var dataUrl = this.context.serviceRoot + '/Processes/hotspots?processId=' + this.state.processDef.id;
      dataUrl += '&span=' + this.state.sample;
      fetch(new Request(dataUrl, {
        method: 'GET',
        headers: { Accept: 'application/json'},
        credentials: 'same-origin'
      }))
      .then(response => {
        return response.json();
      })
      .then(data => {
        $mdwUi.hubLoading(false);
        resolve(data);
      });
    });
  }

  render() {
    var sel;
    if (this.state.processDef.name) {
      sel = this.state.processDef.packageName + '/' + this.state.processDef.name + '.proc';
    }
    return (
      <div>
        <PanelHeader>
          <HeaderLabel title="Process:"/>
          <AssetDropdown id="process-dropdown" placeholder="[Select a process]"
            packages={this.state.packages}
            selected={sel}
            onSelect={this.handleProcessSelect} />

          <HeaderLabel title="Sample:" style={{marginLeft:'10px'}}/>
          <HeaderDropdown id="sample-dropdown" width={100}
            items={['Day','Week','Month']}
            selected={this.state.sample}
            onSelect={this.handleSampleSelect} />
        </PanelHeader>
        <div className="mdw-section" style={{minHeight:'600px'}}>
          <div id="workflow-hotspots">
            {this.state.processDef.id &&
              <div>
                <Workflow 
                  process={this.state.processDef} 
                  data={this.state.data}
                  containerId='workflow-hotspots' 
                  hubBase={this.context.hubRoot} serviceBase={this.context.serviceRoot} />
              </div>
            }
          </div>
        </div>
      </div>
    );
  }
}

ProcessHotspots.contextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string
};

export default ProcessHotspots;

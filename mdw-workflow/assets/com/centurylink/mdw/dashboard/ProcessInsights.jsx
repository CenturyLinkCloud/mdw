import React, {Component} from '../node/node_modules/react/react';
import PropTypes from '../node/node_modules/prop-types';
import {Bar} from '../node/node_modules/react-chartjs-2';
import PanelHeader from '../react/PanelHeader.jsx';
import HeaderLabel from '../react/HeaderLabel.jsx';
import AssetDropdown from '../react/AssetDropdown.jsx';
import HeaderDropdown from '../react/HeaderDropdown.jsx';
import ChartLegend from './ChartLegend.jsx';
import statuses from '../react/statuses';
import {months} from '../react/constants';
import chartOptions from './insightOptions';

class ProcessInsights extends Component {

  constructor(...args) {
    super(...args);
    this.state = {
      packages: [],
      sample: 'Week',
      process: '',
      data: {}
    };
    this.handleProcessSelect = this.handleProcessSelect.bind(this);    
    this.handleSampleSelect = this.handleSampleSelect.bind(this);
    this.getProcessId = this.getProcessId.bind(this);
    this.retrieveData = this.retrieveData.bind(this);
    this.getChartData = this.getChartData.bind(this);
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
      process: assetPath,
      data: {}
    }, () => {
      this.retrieveData()
      .then(data => {
        this.setState({
          packages: this.state.packages,
          sample: this.state.sample,
          process: assetPath,
          data: data
        });
      });
    });
  }

  handleSampleSelect(sampleSize) {
    this.setState({
      packages: this.state.packages,
      sample: sampleSize,
      process: this.state.process
    }, () => {
      this.retrieveData()
      .then(data => {
        this.setState({
          packages: this.state.packages,
          sample: sampleSize,
          process: this.state.process,
          data: data
        });
      });
    });
  }

  getProcessId() {
    for (let i = 0; i < this.state.packages.length; i++) {
      let pkg = this.state.packages[i];
      for (let j = 0; j < pkg.assets.length; j++) {
        let asset = pkg.assets[j];
        if (pkg.name + '/' + asset.name === this.state.process) {
          return asset.id;
        }
      }
    }
  }

  getTimeLabel(time) {
    // TODO handle hourly
    let date = new Date(time);
    if (this.state.sample === 'Day') {
      return 'TODO';
    }
    else {
      return months[date.getMonth()] + ' ' + date.getDate();
    }
  }

  retrieveData() {
    return new Promise(resolve => {
      $mdwUi.hubLoading(true);
      var dataUrl = this.context.serviceRoot + '/Processes/insights?processId=' + this.getProcessId();
      dataUrl += '&trend=completionTime';
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

  getLegendItem(statusName) {
    let status = statuses.process[statusName];
    return {
      id: statusName, 
      name: statusName, 
      color: statuses.shade(status.color, 0.5),
      borderColor: status.color
    };
  }

  getChartData() {
    if (this.state.data.insights) {
      const chartData = {
        labels: this.state.data.insights.map(insight => this.getTimeLabel(insight.time)),
        datasets: []
      };
      var lineDataset;
      if (this.state.data.trend) {
        lineDataset = {
          type: 'line',
          label: 'Completion Time',
          borderColor: '#505050',
          fill: false,
          data: [],
          yAxisID: 'y-axis-line',
          spanGaps: true
        };
        chartData.datasets.push(lineDataset);
      }
      Object.keys(statuses.process).forEach(statusName => {
        let status = statuses.process[statusName];
        let dataset = {
          label: statusName,
          data: [],
          borderColor: status.color,
          backgroundColor: statuses.shade(status.color, 0.5),
          borderWidth: 2,
          yAxisID: 'y-axis-bar'
        };
        chartData.datasets.push(dataset);
        this.state.data.insights.forEach(insight => {
          dataset.data.push(insight.elements[statusName] || 0);
          if (lineDataset) {
            const lineData = this.state.data.trend.find(t => t.time === insight.time);
            lineDataset.data.push(lineData ? lineData.value : null);
          }
        });
      });
      
      return chartData;
    }
  }

  render() {
    let chartData = this.getChartData();

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
        <div className="mdw-section" style={{display:'flex',minHeight:'600px'}}>
            {chartData &&
              <div style={{maxWidth:'300px',marginRight:'20px'}}>
                <div className="mdw-chart-title">
                  Statuses
                </div>
                <ChartLegend
                  items={Object.keys(statuses.process).map(key => this.getLegendItem(key))} />
              </div>
            }
            {chartData &&
              <div style={{height:'100%',width:'100%'}}>
                <Bar data={chartData} 
                  options={chartOptions} />
              </div>
            }
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

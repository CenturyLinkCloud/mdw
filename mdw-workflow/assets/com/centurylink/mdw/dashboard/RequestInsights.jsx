import React, {Component} from '../node/node_modules/react/react';
import PropTypes from '../node/node_modules/prop-types';
import {Bar} from '../node/node_modules/react-chartjs-2';
import PanelHeader from '../react/PanelHeader.jsx';
import HeaderLabel from '../react/HeaderLabel.jsx';
import HeaderDropdown from '../react/HeaderDropdown.jsx';
import ChartLegend from './ChartLegend.jsx';
import statuses from '../react/statuses';
import {months} from '../react/constants';
import chartOptions from './insightOptions';

class RequestInsights extends Component {

  constructor(...args) {
    super(...args);
    this.state = {
      paths: [],
      sample: 'Week',
      path: '',
      data: {}
    };
    this.handlePathSelect = this.handlePathSelect.bind(this);
    this.handleSampleSelect = this.handleSampleSelect.bind(this);
    this.retrieveData = this.retrieveData.bind(this);
    this.getChartData = this.getChartData.bind(this);
  }

  componentDidMount() {
    $mdwUi.hubLoading(true);
    fetch(new Request(this.context.serviceRoot + '/Requests/paths', {
      method: 'GET',
      headers: { Accept: 'application/json'},
      credentials: 'same-origin'
    }))
    .then(response => {
      return response.json();
    })
    .then(paths => {
      this.setState({
        paths: paths,
        sample: this.state.sample,
        path: this.state.path,
        data: {}
      });
      $mdwUi.hubLoading(false);
    });
  }

  handlePathSelect(path) {
    this.setState({
      paths: this.state.paths,
      sample: this.state.sample,
      path: path,
      data: {}
    }, () => {
      this.retrieveData()
      .then(data => {
        this.setState({
          paths: this.state.paths,
          sample: this.state.sample,
          path: path,
          data: data
        });
      });
    });
  }

  handleSampleSelect(sampleSize) {
    this.setState({
      paths: this.state.paths,
      sample: sampleSize,
      path: this.state.path,
      data: {}
    }, () => {
      this.retrieveData()
      .then(data => {
        this.setState({
          paths: this.state.paths,
          sample: sampleSize,
          path: this.state.path,
          data: data
        });
      });
    });
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
      var dataUrl = this.context.serviceRoot + '/Requests/insights?path=' + this.state.path;
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

  getLegendItem(statusCode) {
    let status = statuses.request[statusCode];
    return {
      id: statusCode, 
      name: statusCode + ' - ' + status.message, 
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
        this.state.data.insights.forEach(insight => {
          const lineData = this.state.data.trend.find(t => t.time === insight.time);
          lineDataset.data.push(lineData ? lineData.value : null);
        });
      }
      Object.keys(statuses.request).forEach(statusCode => {
        let status = statuses.request[statusCode];
        let dataset = {
          label: statusCode + ' - ' + status.message,
          data: [],
          borderColor: status.color,
          backgroundColor: statuses.shade(status.color, 0.5),
          borderWidth: 2,
          yAxisID: 'y-axis-bar'
        };
        chartData.datasets.push(dataset);
        this.state.data.insights.forEach(insight => {
          dataset.data.push(insight.elements[statusCode] || 0);
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
          <HeaderLabel title="Path:"/>
          <HeaderDropdown id="path-dropdown" placeholder="[Select a path]"
            items={this.state.paths}
            selected={this.state.path}
            onSelect={this.handlePathSelect} />
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
                  items={Object.keys(statuses.request).map(key => this.getLegendItem(key))} />
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

RequestInsights.contextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string
};

export default RequestInsights;

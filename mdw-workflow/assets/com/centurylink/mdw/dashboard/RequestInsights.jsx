import React, {Component} from '../node/node_modules/react/react';
import PropTypes from '../node/node_modules/prop-types';
import {AsyncTypeahead} from '../node/node_modules/react-bootstrap-typeahead';
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
      requestType: 'Inbound',
      path: '',
      data: {}
    };
    this.handleSampleSelect = this.handleSampleSelect.bind(this);
    this.handleRequestTypeSelect = this.handleRequestTypeSelect.bind(this);
    this.getStart = this.getStart.bind(this);
    this.findPaths = this.findPaths.bind(this);
    this.handlePathSelect = this.handlePathSelect.bind(this);
    this.retrieveData = this.retrieveData.bind(this);
    this.getChartData = this.getChartData.bind(this);
  }

  handleSampleSelect(sampleSize) {
    this.setState({
      paths: this.state.paths,
      sample: sampleSize,
      requestType: this.state.requestType,
      path: this.state.path,
      data: {}
    }, () => {
      this.retrieveData()
      .then(data => {
        this.setState({
          paths: this.state.paths,
          sample: sampleSize,
          requestType: this.state.requestType,
          path: this.state.path,
          data: data
        });
      });
    });
  }

  handleRequestTypeSelect(requestType) {
    this.setState({
      paths: this.state.paths,
      sample: this.state.sample,
      requestType: requestType,
      path: this.state.path,
      data: {}
    }, () => {
      this.retrieveData()
      .then(data => {
        this.setState({
          paths: this.state.paths,
          sample: this.state.sample,
          requestType: requestType,
          path: this.state.path,
          data: data
        });
      });
    });
  }

  getStart() {
    var spanMs = 24 * 3600 * 1000; // one day
    if (this.state.sample === 'Week') {
      spanMs = spanMs * 6;
    }
    else if (this.state.sample === 'Month') {
      spanMs = spanMs * 29;
    }
    return new Date(new Date().getTime() - spanMs);
  }

  findPaths(input) {
    this.pathsLoading = true;
    var url = this.context.serviceRoot + '/Requests/paths' +
            '?Starting=' + this.getStart().toISOString() +
            '&direction=' + (this.state.requestType === 'Outbound' ? 'out' : 'in') + 
            '&find=' + input;

    fetch(new Request(url, {
      method: 'GET',
      headers: { Accept: 'application/json'},
      credentials: 'same-origin'
    }))
    .then(response => {
      return response.json();
    })
    .then(data => {
      data.paths.forEach(p => p.path = p.path.replace(/->/, '→ '));
      this.setState({
        paths: data.paths,
        sample: this.state.sample,
        requestType: this.state.requestType,
        path: this.state.path,
        data: {}
      }, () => this.pathsLoading = false);
    });
  }

  handlePathSelect(paths) {
    var path = paths.constructor === Array ? paths[0] : paths;
    this.setState({
      paths: this.state.paths,
      sample: this.state.sample,
      requestType: this.state.requestType,
      path: path ? path.path : '',
      data: {}
    }, () => {
      this.retrieveData()
      .then(data => {
        this.setState({
          paths: this.state.paths,
          sample: this.state.sample,
          requestType: this.state.requestType,
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
      var dataUrl = this.context.serviceRoot + '/Requests/insights?path=' + 
              encodeURI(this.state.path.replace(/→ /, '->'));
      dataUrl += '&trend=completionTime';
      dataUrl += '&span=' + this.state.sample;
      dataUrl += '&direction=' + (this.state.requestType === 'Outbound' ? 'out' : 'in');
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
          <HeaderLabel title="Sample:"/>
          <HeaderDropdown id="sample-dropdown" width={100}
            items={['Day','Week','Month']}
            selected={this.state.sample}
            onSelect={this.handleSampleSelect} />
          <HeaderLabel title="Requests:" />
          <HeaderDropdown id="request-dropdown" width={100}
            items={['Inbound','Outbound','Master']}
            selected={this.state.requestType}
            onSelect={this.handleRequestTypeSelect} />
          <HeaderLabel title="Path:" />
          <AsyncTypeahead className="mdw-typeahead"
            autoFocus 
            placeholder="Enter a path or method..."
            clearButton
            onSearch={this.findPaths} 
            options={this.state.paths}
            labelKey="path"
            isLoading={this.pathsLoading || false}
            onChange={this.handlePathSelect}  />
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

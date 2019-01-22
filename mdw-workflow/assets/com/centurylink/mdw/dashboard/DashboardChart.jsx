import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import {Doughnut, Bar, Line} from '../node/node_modules/react-chartjs-2';
import ChartHeader from './ChartHeader.jsx';
import ChartLegend from './ChartLegend.jsx';

class DashboardChart extends Component {

  constructor(...args) {
    super(...args);
    // constants (TODO: parameterize)
    this.initialSelect = 5;
    this.maxTops = 50;
    this.defaultTimespan = 'Week';
    this.chartColors = ['#3366CC','#FF9900','#DC3912','#109618','#990099','#3B3EAC','#0099C6','#DD4477','#66AA00','#B82E2E','#316395','#994499','#22AA99','#AAAA11','#6633CC','#E67300','#8B0707','#329262','#5574A6','#3B3EAC'];
    this.chartOptions = {legend: {display: false, position: 'bottom'}};

    this.state = {
      timespan: this.defaultTimespan,
      breakdown: this.props.breakdownConfig.breakdowns[0].name,
      tops: [],
      selected: [],
      filters: this.props.breakdownConfig.filters || {},
      data: {}
    };
    if (this.state.filters.Ending) {
      this.state.filters.Ending = this.getDefaultEnd();
    }
    this.getBreakdown = this.getBreakdown.bind(this);
    this.getDefaultEnd = this.getDefaultEnd.bind(this);
    this.getStart = this.getStart.bind(this);
    this.setTops = this.setTops.bind(this);
    this.setSelected = this.setSelected.bind(this);
    this.handleTimespanChange = this.handleTimespanChange.bind(this);
    this.handleBreakdownChange = this.handleBreakdownChange.bind(this);
    this.handleTopSelect = this.handleTopSelect.bind(this);
    this.handleSelectCancel = this.handleSelectCancel.bind(this);
    this.handleSelectApply = this.handleSelectApply.bind(this);
    this.handleFilterChange = this.handleFilterChange.bind(this);
    this.handleFilterReset = this.handleFilterReset.bind(this);
    this.handleDownload = this.handleDownload.bind(this);
    this.buildUrl = this.buildUrl.bind(this);
    this.update = this.update.bind(this);
    this.updateChart = this.updateChart.bind(this);
    this.retrieveTops = this.retrieveTops.bind(this);
    this.retrieveData = this.retrieveData.bind(this);
    this.handleOverallClick = this.handleOverallClick.bind(this);
    this.handleDataClick = this.handleDataClick.bind(this);
  }

  // selected breakdown object from breakdownConfig
  getBreakdown() {
    return this.props.breakdownConfig.breakdowns.find(bd => bd.name === this.state.breakdown);
  }

  getDefaultEnd() {
    var timespan = this.defaultTimespan;
    if (this.state && this.state.timespan) {
      timespan = this.state.timespan;
    }
    var end = new Date();
    end.setMinutes(0);
    end.setSeconds(0);
    end.setMilliseconds(0);
    if (timespan === 'Week' || timespan === 'Month') {
      end.setHours(0);
    }
    return end;
  }

  getStart() {
    var spanMs = 24 * 3600 * 1000; // one day
    if (this.state.timespan === 'Week') {
      spanMs = spanMs * 6;
    }
    else if (this.state.timespan === 'Month') {
      spanMs = spanMs * 29;
    }
    return new Date(this.state.filters.Ending.getTime() - spanMs);
  }

  setTops(tops, selected) {
    return new Promise(resolve => {
      this.setState({
        timespan: this.state.timespan,
        breakdown: this.state.breakdown,
        tops: tops,
        selected: selected ? selected : this.state.selected,
        filters: this.state.filters,
        data: {}
      }, () => {
        if (selected) {
          this.previousSelected = this.state.selected;
        }
        resolve();
      });
    });
  }

  setSelected(selected) {
    return new Promise(resolve => {
      this.setState({
        timespan: this.state.timespan,
        breakdown: this.state.breakdown,
        tops: this.state.tops,
        selected: selected,
        filters: this.state.filters,
        data: {}
      }, () => {
        resolve();
      });
    });
  }

  handleTimespanChange(timespan) {
    this.setState({
      timespan: timespan,
      breakdown: this.state.breakdown,
      tops: this.state.tops,
      selected: this.state.selected,
      filters: this.state.filters,
      data: {}
    }, this.update);
  }

  handleBreakdownChange(breakdown) {
    this.setState({
      timespan: this.state.timespan,
      breakdown: breakdown,
      tops: this.state.tops,
      selected: this.state.selected,
      filters: this.state.filters,
      data: {}
    }, this.update);
  }

  handleTopSelect(top, isSelected) {
    var selected = this.state.selected.slice(0);
    if (isSelected) {
      selected.push(top);
    }
    else {
      const idx = selected.findIndex(t => t.id === top.id);
      selected.splice(idx, 1);
    }
    this.setSelected(selected);
  }

  handleSelectCancel() {
    if (this.previousSelected) {
      this.setSelected(this.previousSelected, this.updateChart());
    }
  }

  handleSelectApply() {
    this.updateChart();
  }

  handleFilterChange(filters) {
    if (!filters.Ending) {
      filters.Ending = this.getDefaultEnd();
    }
    if (this.state.timespan === 'Hours') {
      filters.Ending.setHours(new Date().getHours());
    }
    else {
      filters.Ending.setHours(0);
    }
    this.setState({
      timespan: this.state.timespan,
      breakdown: this.state.breakdown,
      tops: this.state.tops,
      selected: this.state.selected,
      filters: filters,
      data: {}
    }, this.update);
  }

  handleFilterReset() {
    var filters = this.props.breakdownConfig.filters || {};
    if (filters.Ending) {
      filters.Ending = this.getDefaultEnd();
    }
    this.handleFilterChange(filters);
  }

  handleDownload() {
    const breakdown = this.getBreakdown();
    if (breakdown) {
      var downloadUrl = this.buildUrl(this.context.serviceRoot + breakdown.data);
      if (breakdown.instancesParam) {
        downloadUrl += '&' + breakdown.instancesParam + '=%5B' + this.state.selected.map(sel => {
          return breakdown.instancesParam === 'statuses' ? sel.name : sel.id;
        }).join() + '%5D';
      }
      location = downloadUrl + '&DownloadFormat=xlsx';
    }
  }

  componentDidMount() {
    this.update();
  }

  buildUrl(base) {
    const breakdown = this.getBreakdown();
    var url = base + (breakdown.data.indexOf('?') >= 0 ? '&' : '?');
    url += 'Starting=' + this.getStart().toISOString();
    Object.keys(this.state.filters).forEach(key => {
      let val = this.state.filters[key];
      if (val) {
        if (val instanceof Date) {
          url += '&' + key + '=' + val.toISOString();
        }
        else {
          url += '&' + key + '=' + val;
        }
      }
    });
    return url;
  }

  // retrieves tops, resets selected, then retrieves data and updates state
  update() {
    this.retrieveTops()
    .then(tops => {
      this.setTops(tops, tops.slice(0, (this.initialSelect > tops.length ? tops.length : this.initialSelect)));
    })
    .then(() => {
      this.updateChart();
    });
  }

  updateChart() {
    this.retrieveData()
    .then(data => {
      this.setState({
        timespan: this.state.timespan,
        breakdown: this.state.breakdown,
        tops: this.state.tops,
        selected: this.state.selected,
        filters: this.state.filters,
        data: data
      });
    });
  }

  // updates state and returns a promise
  retrieveTops() {
    return new Promise(resolve => {
      const breakdown = this.getBreakdown();
      if (breakdown && breakdown.tops) {
        var topsUrl = this.buildUrl(this.context.serviceRoot + breakdown.tops);
        topsUrl += '&max=' + this.maxTops;
        fetch(new Request(topsUrl, {
          method: 'GET',
          headers: { Accept: 'application/json'},
          credentials: 'same-origin'
        }))
        .then(response => {
          return response.json();
        })
        .then(tops => {
          resolve(tops);
        });
      }
      else {
        resolve([]);
      }
    });
  }

  retrieveData() {
    return new Promise(resolve => {
      const breakdown = this.getBreakdown();
      if (breakdown) {
        var dataUrl = this.buildUrl(this.context.serviceRoot + breakdown.data);
        if (breakdown.instancesParam) {
          dataUrl += '&' + breakdown.instancesParam + '=%5B' + this.state.selected.map(sel => {
            return breakdown.instancesParam === 'statuses' ? sel.name : sel.id;
          }).join() + '%5D';
        }
        fetch(new Request(dataUrl, {
          method: 'GET',
          headers: { Accept: 'application/json'},
          credentials: 'same-origin'
        }))
        .then(response => {
          return response.json();
        })
        .then(data => {
          resolve(data);
        });
      }
      else {
        resolve({});
      }
    });
  }

  getOverallData() {
    const breakdown = this.getBreakdown();
    const overallData = {labels: [], datasets: [{label: 'Overall', data: [], backgroundColor: []}]};
    this.state.selected.forEach((sel, i) => {
      let label = breakdown.summaryChart === 'bar' ? '' : sel.name;
      overallData.labels.push(label);
      overallData.datasets[0].data.push(sel.value);
      overallData.datasets[0].backgroundColor.push(this.chartColors[i]);
    }, this);
    return overallData;
  }

  getLineData() {
    const lineData = {labels: [], datasets: []};
    var datasets = {}; // id to dataset
    if (this.state.selected.length > 0) {
      this.state.selected.forEach((sel, i) => {
        let dataset = {label: sel.name, borderColor: this.chartColors[i], data: [], fill: false};
        datasets[sel.id] = dataset;
        lineData.datasets.push(dataset);
        Object.keys(this.state.data).forEach(key => {
          if (i === 0) {
            lineData.labels.push(key);
          }
          const aggs = this.state.data[key];
          const selAgg = aggs.find(agg => agg.id === sel.id);
          dataset.data.push(selAgg ? selAgg.value : 0);
        }, this);
      }, this);
    }
    else { // eg: total
      let dataset = {borderColor: this.chartColors[0], data: [], fill: false};
      lineData.datasets.push(dataset);
      Object.keys(this.state.data).forEach(key => {
        lineData.labels.push(key);
        let point = this.state.data[key][0];
        if (point) {
          dataset.data.push(point.value);
        }
        else {
          dataset.data.push(0);
        }
      }, this);
    }
    // console.log("DATA: " + JSON.stringify(lineData, null, 2));
    return lineData;
  }

  handleOverallClick(chartElements) {
    if (chartElements && chartElements.length === 1) {
      const sel = this.state.selected[chartElements[0]._index];
      var procFilter = sessionStorage.getItem('processFilter');
      if (procFilter)
        procFilter = JSON.parse(procFilter);
      else
        procFilter = {};
      procFilter.processId = sel.id;
      if (procFilter.processId) {
        var procSpec = sel.name;
        procFilter.master = this.state.filters.Master ? this.state.filters.Master : false;
        const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
        const start = this.getStart();
        procFilter.startDate = start.getFullYear().toString() + '-' + months[start.getMonth()] + '-' + start.getDate();
        procFilter.status = this.state.filters.Status ? this.state.filters.Status : '[Any]';
        sessionStorage.setItem('processFilter', JSON.stringify(procFilter));
        sessionStorage.setItem('processSpec', procSpec);
      }
    }
    location = this.context.hubRoot + '/' + this.props.list;
  }

  handleDataClick(chartElements) {
    if (chartElements && chartElements.length === 1) {
      // console.log("DATASET INDEX: " + chartElements[0]._datasetIndex);
    }
  }

  render() {
    const breakdown = this.getBreakdown();
    return (
      <div>
        <ChartHeader title={this.props.title}
          breakdownConfig={this.props.breakdownConfig}
          breakdown={this.state.breakdown}
          timespan={this.state.timespan}
          list={this.props.list}
          tops={this.state.tops}
          selected={this.state.selected}
          filters={this.state.filters}
          filterOptions={this.props.breakdownConfig.filterOptions}
          onTimespanChange={this.handleTimespanChange}
          onBreakdownChange={this.handleBreakdownChange}
          onSelect={this.handleTopSelect}
          onSelectCancel={this.handleSelectCancel}
          onSelectApply={this.handleSelectApply}
          onFilterChange={this.handleFilterChange}
          onFilterReset={this.handleFilterReset}
          onDownload={this.handleDownload}/>
        <div className="mdw-section" style={{display:'flex'}}>
          {breakdown.tops &&
            <div style={{maxWidth:'303px',maxHeight:'303px'}}>
              {(!breakdown.summaryChart || breakdown.summaryChart === 'donut') &&
                <Doughnut
                  data={this.getOverallData()}
                  options={this.chartOptions}
                  width={250} height={250}
                  getElementAtEvent={this.handleOverallClick} />
              }
              {breakdown.summaryChart === 'bar' &&
                <Bar
                  data={this.getOverallData()}
                  options={this.chartOptions}
                  width={250} height={250}
                  getElementAtEvent={this.handleOverallClick} />
              }
              <ChartLegend
                colors={this.chartColors}
                tops={this.state.tops}
                selected={this.state.selected} />
            </div>
          }
          {!breakdown.tops &&
            <div className="mdw-chart-title" style={{width:'320px'}}>
              {breakdown.name}
            </div>
          }
          <div style={{height:'100%',width:'100%'}}>
            <Line
              data={this.getLineData()}
              options={this.chartOptions}
              getElementAtEvent={this.handleDataClick} />
          </div>
        </div>
      </div>
    );
  }
}

DashboardChart.contextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string
};

export default DashboardChart;

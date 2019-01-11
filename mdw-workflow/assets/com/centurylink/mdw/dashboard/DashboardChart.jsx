import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import {Doughnut, Line} from '../node/node_modules/react-chartjs-2';
import ChartHeader from './ChartHeader.jsx';
import ChartLegend from './ChartLegend.jsx';

class DashboardChart extends Component {

  constructor(...args) {
    super(...args);
    // constants (TODO: parameterize)
    this.initialSelect = 5;
    this.maxTops = 50;
    this.defaultTimespan = 'Week';
    this.chartColors = ['#3366CC','#DC3912','#FF9900','#109618','#990099','#3B3EAC','#0099C6','#DD4477','#66AA00','#B82E2E','#316395','#994499','#22AA99','#AAAA11','#6633CC','#E67300','#8B0707','#329262','#5574A6','#3B3EAC'];
    this.chartOptions = {legend: {display: false, position: 'bottom'}};

    this.state = {
      timespan: this.defaultTimespan,
      breakdown: this.props.breakdownConfig.breakdowns[0].name,
      tops: [],
      selected: [],
      filters: {ending: this.getDefaultEnd(), status: ''},
      data: {}
    };
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
    this.retrieveTops = this.retrieveTops.bind(this);
    this.update = this.update.bind(this);
    this.retrieveData = this.retrieveData.bind(this);
    this.updateChart = this.updateChart.bind(this);
    this.setDonutRef = this.setDonutRef.bind(this);
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
    return new Date(this.state.filters.ending.getTime() - spanMs);
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
      this.setSelected(this.previousSelected);
    }
  }

  handleSelectApply() {
    this.updateChart();
  }

  handleFilterChange(filters) {
    if (filters.ending) {
      if (this.state.timespan === 'Hours') {
        filters.ending.setHours(new Date().getHours());
      }
      else {
        filters.ending.setHours(0);
      }
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
    this.handleFilterChange({
      ending: this.getDefaultEnd(),
      status: ''
    });
  }

  componentDidMount() {
    this.update();
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
      if (breakdown) {
        var topsUrl = this.context.serviceRoot + breakdown.throughput;
        topsUrl += (breakdown.throughput.indexOf('?') >= 0 ? '&' : '?');
        topsUrl += 'max=' + this.maxTops + '&startDt=' + this.getStart().toISOString();
        if (this.state.filters.ending) {
          topsUrl += '&endDt=' + this.state.filters.ending.toISOString();
        }
        if (this.state.filters.status) {
          topsUrl += '&status=' + this.state.filters.status;
        }
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
        var dataUrl = this.context.serviceRoot + breakdown.data;
        dataUrl += (breakdown.data.indexOf('?') >= 0 ? '&' : '?');
        dataUrl += 'startDt=' + this.getStart().toISOString();
        if (this.state.filters.ending) {
          dataUrl += '&endDt=' + this.state.filters.ending.toISOString();
        }
        if (this.state.filters.status) {
          dataUrl += '&status=' + this.state.filters.status;
        }
        if (breakdown.instancesParam) {
          dataUrl += '&' + breakdown.instancesParam + '=%5B' + this.state.selected.map(sel => sel.id).join() + '%5D';
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

  getDonutData() {
    const donutData = {labels: [], datasets: [{data: [], backgroundColor: []}]};
    this.state.selected.forEach((sel, i) => {
      donutData.labels.push(sel.name);
      donutData.datasets[0].data.push(sel.count);
      donutData.datasets[0].backgroundColor.push(this.chartColors[i]);
    }, this);
    return donutData;
  }

  getLineData() {
    const lineData = {labels: [], datasets: []};
    var datasets = {}; // id to dataset
    this.state.selected.forEach((sel, i) => {
      var dataset = {label: sel.name, borderColor: this.chartColors[i], data: [], fill: false};
      datasets[sel.id] = dataset;
      lineData.datasets.push(dataset);
      Object.keys(this.state.data).forEach(key => {
        if (i === 0) {
          lineData.labels.push(key);
        }
        const counts = this.state.data[key];
        const selCount = counts.find(ct => ct.id === sel.id);
        dataset.data.push(selCount ? selCount.count : 0);
      }, this);
    }, this);
    return lineData;
  }

  setDonutRef(ref) {
    this.donutRef = ref;
  }

  render() {
    return (
      <div>
        <ChartHeader title={this.props.title}
          breakdownConfig={this.props.breakdownConfig}
          statuses={this.props.statuses}
          breakdown={this.state.breakdown}
          timespan={this.state.timespan}
          list={this.props.list}
          tops={this.state.tops}
          selected={this.state.selected}
          filters={this.state.filters}
          onTimespanChange={this.handleTimespanChange}
          onBreakdownChange={this.handleBreakdownChange}
          onSelect={this.handleTopSelect}
          onSelectCancel={this.handleSelectCancel}
          onSelectApply={this.handleSelectApply}
          onFilterChange={this.handleFilterChange}
          onFilterReset={this.handleFilterReset} />
        <div className="mdw-section" style={{display:'flex'}}>
          <div>
            <Doughnut ref={this.setDonutRef}
              data={this.getDonutData()}
              options={this.chartOptions}
              width={250} height={250}/>
            <ChartLegend
              colors = {this.chartColors}
              tops={this.state.tops}
              selected={this.state.selected} />
          </div>
          <div style={{height:'100%',width:'100%'}}>
            <Line data={this.getLineData()} options={this.chartOptions} />
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

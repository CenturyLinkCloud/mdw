import React, {Component} from '../node/node_modules/react';
import Value from '../react/Value.jsx';

class Values extends Component {
    
  constructor(...args) {
    super(...args);
    this.state = { values: [] };
  }  

  componentDidMount() {
    fetch(new Request('/mdw/services/Tasks/' + this.props.task.id + '/values', {
      method: 'GET',
      headers: { Accept: 'application/json'}
    }))
    .then(response => {
      return response.json();
    })
    .then(vals => {
      var values = [];
      Object.keys(vals).forEach(key => {
        vals[key].name = key
        values.push(vals[key]);
      });
      values.sort((val1, val2) => {
        var diff = val1.sequence - val2.sequence;
        if (diff === 0) {
          var label1 = val1.label ? val1.label : val1.name;
          var label2 = val2.label ? val2.label : val2.name;
          return label1.toLowerCase().localeCompare(label2.toLowerCase());
        }
        else {
          return diff;
        }
      });      
      this.setState({
        values: values
      });
    });    
  }
  
  render() {
    return (
      <form name="valuesForm" className="form-horizontal" role="form">
        {this.state.values.map(value => {
          return <Value value={value} key={value.name} />
        })};
      </form>
    );
  }
}

export default Values;  
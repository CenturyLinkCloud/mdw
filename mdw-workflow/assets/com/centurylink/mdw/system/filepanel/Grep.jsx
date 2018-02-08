import React, {Component} from '../../node/node_modules/react';
import {Button, Glyphicon} from '../../node/node_modules/react-bootstrap';
import '../../node/node_modules/style-loader!./filepanel.css';

class Grep extends Component {
  constructor(...args) {
    super(...args);
    this.state = { pattern: '', files: '' };
    this.handleClick = this.handleClick.bind(this);    
    this.handleChange = this.handleChange.bind(this);
  }

  handleClick(event) {
    if (event.currentTarget.name === 'grep') {
      console.log('grep');
    }
  }
  
  handleChange(event) {
    if (event.currentTarget.name === 'pattern') {
      this.setState({
        pattern: event.currentTarget.value,
        files: this.state.files
      });
    }
    else if (event.currentTarget.name === 'files') {
      this.setState({
        pattern: this.state.pattern,
        files: event.currentTarget.value
      });
    }
  }
  
  render() {
    return (
      <div className="fp-grep">
        <div>
          <input name="pattern" type="text" placeholder="Search Expression" />
        </div>
        <div>
          <input name="files" type="text" placeholder="Glob Pattern" />
          <button name="grep" value="grep" onClick={this.handleClick}>Grep</button>
        </div>
      </div>
    )
  };
}

export default Grep; 
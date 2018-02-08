import React, {Component} from '../../node/node_modules/react';
import {Button, Glyphicon} from '../../node/node_modules/react-bootstrap';
import '../../node/node_modules/style-loader!./filepanel.css';

class Grep extends Component {
  constructor(...args) {
    super(...args);
    this.state = { find: '', glob: '' };
    this.handleClick = this.handleClick.bind(this);    
    this.handleChange = this.handleChange.bind(this);
  }

  handleClick(event) {
    if (event.currentTarget.name === 'grep') {
      this.props.onGrep(this.state.find, this.state.glob);
    }
  }
  
  handleChange(event) {
    if (event.currentTarget.name === 'find') {
      this.setState({
        find: event.currentTarget.value,
        glob: this.state.glob
      });
    }
    else if (event.currentTarget.name === 'glob') {
      this.setState({
        find: this.state.find,
        glob: event.currentTarget.value
      });
    }
  }
  
  render() {
    return (
      <div className="fp-grep">
        <div>
          <input name="find" 
            type="text" 
            placeholder="Search Expression"
            onChange={this.handleChange} />
        </div>
        <div>
          <input name="glob" 
            type="text" 
            placeholder="File Glob Pattern" 
            onChange={this.handleChange} />
          <button name="grep" 
            value="grep" 
            disabled={!this.state.find || !this.state.glob || !this.props.item.path} 
            onClick={this.handleClick}>
            Grep
          </button>
        </div>
      </div>
    )
  };
}

export default Grep; 
import React, {Component} from '../../node/node_modules/react';
import {Button, Glyphicon} from '../../node/node_modules/react-bootstrap';
import '../../node/node_modules/style-loader!./filepanel.css';

class Search extends Component {
  constructor(...args) {
    super(...args);
    this.state = { search: '', start: 0 };
    this.handleClick = this.handleClick.bind(this);    
    this.handleChange = this.handleChange.bind(this);
    this.handleKeyPress = this.handleKeyPress.bind(this);
  }

  handleClick(event) {
    const params = {
      'find': this.state.search,
      'backward': event.currentTarget.name === 'backward'
    };
    this.props.onAction('search', params);
  }
  
  handleChange(event) {
    if (event.currentTarget.name === 'search') {
      const find = event.currentTarget.value;
      this.setState({
        search: find
      });
      if (find) {
        this.props.onAction('find', {find: find});
      }
    }
  }
  
  handleKeyPress(event) {
    if (event.currentTarget.name === 'search' && event.key === 'Enter') {
      this.props.onAction('search', {find: this.state.search});
    }
  }
  
  render() {
    return (
      <div className="fp-search">
        <div>
          <input name="search"
            type="text" 
            placeholder="Search" 
            value={this.state.search}
            onChange={this.handleChange}
            onKeyPress={this.handleKeyPress}/>
          <Button name="backward" 
            className="fp-icon-btn"
            style={{marginLeft:'0'}}
            title="Backward"
            disabled={!this.state.search}
            onClick={this.handleClick}>
            <Glyphicon glyph="chevron-up" />
          </Button>
          <Button name="forward" 
            className="fp-icon-btn"
            style={{marginLeft:'3px'}}
            title="Forward"
            disabled={!this.state.search}
            onClick={this.handleClick}>
            <Glyphicon glyph="chevron-down" />
          </Button>
        </div>
      </div>
    )
  };
}

export default Search;
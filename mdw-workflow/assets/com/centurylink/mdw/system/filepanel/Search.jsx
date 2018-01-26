import React, {Component} from '../../node/node_modules/react';
import {Button, Glyphicon} from '../../node/node_modules/react-bootstrap';
import '../../node/node_modules/style-loader!./filepanel.css';

class Search extends Component {
  constructor(...args) {
    super(...args);
    this.state = { search: '' };
    this.handleClick = this.handleClick.bind(this);    
    this.handleChange = this.handleChange.bind(this);
    this.handleKeyPress = this.handleKeyPress.bind(this);
  }

  handleClick(event) {
    const isClear = event.currentTarget.className === 'fp-clear';
    if (isClear) {
      this.setState({search: ''});
    }
    const params = {
      'find': isClear ? {search: ''} : this.state.search,
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
      if (this.props.options.searchWhileTyping) {
        if (find) {
          if (find.length >= this.props.options.searchMinLength) {
            this.props.onAction('find', {find: find, start: undefined});
          }
          else {
            this.props.onAction('search', {find: {search: ''}});
          }
        }
        else {
          this.props.onAction('search', {find: {search: ''}});
        }
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
          <span name='clear' 
            className='fp-clear' 
            onClick={this.handleClick}>×</span>        
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
          <span className="fp-search-msg">
            {this.props.message}
          </span>
        </div>
      </div>
    )
  };
}

export default Search;
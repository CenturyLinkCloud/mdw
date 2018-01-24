import React, {Component} from '../../node/node_modules/react';
import {Button, Glyphicon} from '../../node/node_modules/react-bootstrap';
import '../../node/node_modules/style-loader!./filepanel.css';

class Search extends Component {
  constructor(...args) {
    super(...args);
    this.state = { search: '', start: 0 };
    this.handleClick = this.handleClick.bind(this);    
    this.handleChange = this.handleChange.bind(this);    
  }

  handleClick(event) {
    const params = {
      'find': this.state.search
    };
    if (event.currentTarget.name === 'backward')
      params.backward = true;
    this.props.onAction('search', params);
  }
  
  handleChange(event) {
    if (event.currentTarget.name === 'search') {
      this.setState({
        search: event.currentTarget.value
      });
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
            onChange={this.handleChange} />
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
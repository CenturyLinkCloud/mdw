import React, {Component} from '../node/node_modules/react';
import {Popover} from '../node/node_modules/react-bootstrap/lib';
import Dropdown from './Dropdown.jsx';

class SortPop extends Component {

  constructor(...args) {
    super(...args);
    this.handleChange = this.handleChange.bind(this);
  }

  handleChange(sort) {
    if (this.props.onSortChange) {
      this.props.onSortChange(sort);
    }
  }

  render() {
    const {sorts, sort, onSortChange, ...popProps} = this.props; // eslint-disable-line no-unused-vars
    return (
      <Popover {...popProps} id="sort-pop">
        <div style={{width:'120px'}}>
          <Dropdown id="sort-drop"
            items={this.props.sorts}
            selected={this.props.sort}
            onSelect={sel => this.handleChange(sel)} />
        </div>
      </Popover>
    );
  }
}

export default SortPop;

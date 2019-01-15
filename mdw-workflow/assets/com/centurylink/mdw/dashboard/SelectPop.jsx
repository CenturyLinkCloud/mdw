import React, {Component} from '../node/node_modules/react';
import {Popover} from '../node/node_modules/react-bootstrap';

class SelectPop extends Component {

  constructor(...args) {
    super(...args);
    this.isSelected = this.isSelected.bind(this);
    this.select = this.select.bind(this);
    this.deselect = this.deselect.bind(this);
    this.cancel = this.cancel.bind(this);
    this.apply = this.apply.bind(this);
  }

  getLabel(top) {
    var label = top.name;
    if (top.value) {
      label += ' (' + top.value + ')';
    }
    return label;
  }

  getTitle(top) {
    if (top.packageName) {
      var title = top.packageName + '/' + top.name;
      if (top.version) {
        title += ' v' + top.version;
      }
      return title;
    }
  }

  isSelected(top) {
    return this.props.selected.find(sel => sel.id === top.id);
  }

  select(top) {
    if (this.props.onSelect) {
      this.props.onSelect(top, true);
    }
  }

  deselect(top) {
    if (this.props.onSelect) {
      this.props.onSelect(top, false);
    }
  }

  cancel() {
    if (this.props.onCancel) {
      this.props.onCancel();
    }
  }

  apply() {
    if (this.props.onApply) {
      this.props.onApply();
    }
  }

  render() {
    const {tops, onCancel, onApply, ...popProps} = this.props; // eslint-disable-line no-unused-vars
    return (
      <Popover {...popProps} id="select-pop">
        <div>
          <div>
            <label className="mdw-label">{this.props.label}:</label>
            <div className="mdw-bordered-menu" style={{display:'inline-flex',overflowX:'auto'}}>
              <ul className="dropdown-menu mdw-popover-menu mdw-check-menu" style={{marginRight:'10px'}}>
              {
                tops.map((top, i) => {
                  const selected = this.isSelected(top);
                  return (
                    <li key={i}>
                      {selected &&
                        <div style={{whiteSpace:'nowrap'}}>
                          <i className="glyphicon glyphicon-ok"></i>
                          <span className="mdw-drop-selected" style={{marginLeft:'5px'}}
                            title={this.getTitle(top)}
                            onClick={() => this.deselect(top)}>
                            {this.getLabel(top)}
                          </span>
                        </div>
                      }
                      {!selected &&
                        <span style={{marginLeft:'18px'}}
                          title={this.getTitle(top)}
                          onClick={() => this.select(top)}>
                          {this.getLabel(top)}
                        </span>
                      }
                    </li>
                  );
                })
              }
              </ul>
            </div>
          </div>
          <div className="mdw-vsm-indent">
            <button type="button" className="btn btn-primary mdw-btn"
              onClick={this.apply}>
              Apply
            </button>
            <button type="button" className="btn mdw-btn mdw-cancel-btn mdw-float-right"
              onClick={this.cancel}>
              Cancel
            </button>
          </div>
        </div>
      </Popover>
    );
  }
}

export default SelectPop;

import React, {Component} from '../node/node_modules/react';
import {Popover} from '../node/node_modules/react-bootstrap';

class SelectPop extends Component {

  constructor(...args) {
    super(...args);
    this.state = { selected: this.props.selected };
    this.getLabel = this.getLabel.bind(this);
    this.getTitle = this.getTitle.bind(this);
    this.isSelected = this.isSelected.bind(this);
    this.select = this.select.bind(this);
    this.deselect = this.deselect.bind(this);
    this.apply = this.apply.bind(this);
  }

  getLabel(top) {
    // TODO
    return top.name;
  }

  getTitle(top) {
    // TODO
    return top.name;
  }

  isSelected(top) {
    // TODO
    if (top)
      return false;
  }

  select(top) {
    console.log("SELECT: " + JSON.stringify(top, null, 2)); // eslint-disable-line no-console
  }

  deselect(top) {
    console.log("DESELECT: " + JSON.stringify(top, null, 2)); // eslint-disable-line no-console
  }

  apply() {
    console.log("APPLY"); // eslint-disable-line no-console
  }

  render() {
    const {tops, ...popProps} = this.props;
    return (
      <Popover {...popProps} id="select-pop">
        <div>
          <div>
            <label className="mdw-label">{this.props.label}:</label>
            <div className="mdw-bordered-menu" style={{display:'inline-flex',overflowX:'auto'}}>
              <ul className="dropdown-menu mdw-popover-menu mdw-check-menu">
              {
                tops.map((top, i) => {
                  const selected = this.isSelected(top);
                  return (
                    <li key={i}>
                      {selected &&
                        <div className="mdw-checked">
                          <i className="glyphicon glyphicon-ok"></i>
                          <a className="mdw-drop-selected" href=""
                            title={this.getTitle(top)}
                            onClick={() => this.deselect(top)}>
                            {this.getLabel(top)}
                          </a>
                        </div>
                      }
                      {!selected &&
                        <a href=""
                          title={this.getTitle(top)}
                          onClick={() => this.select(top)}>
                          {this.getLabel(top)}
                        </a>
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
              onClick={() => document.body.click()}>
              Cancel
            </button>
          </div>
        </div>
      </Popover>
    );
  }
}

export default SelectPop;

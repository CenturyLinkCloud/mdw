import React, {Component} from '../node/node_modules/react';
import {Popover} from '../node/node_modules/react-bootstrap';

class InfoPop extends Component {

  constructor(...args) {
    super(...args);
  }

  render() {
    const {groups, ...popProps} = this.props; // eslint-disable-line no-unused-vars
    return (
      <Popover {...popProps} id="info-pop">
        <div>
          <ul className="mdw-legend">
            {
              groups.map((group, i) => {
                return (
                  <li key={i}>
                    <div className="mdw-legend-item" style={{backgroundColor: group.props.color}}></div>
                    <div style={{marginTop:'10px'}}>
                      {group.name}
                    </div>
                  </li>
                );
              })
            }
          </ul>
        </div>
      </Popover>
    );
  }
}

export default InfoPop;

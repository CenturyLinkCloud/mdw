import React, {Component} from '../node/node_modules/react';
import {Popover} from '../node/node_modules/react-bootstrap';

class OtherPop extends Component {

  constructor(...args) {
    super(...args);
  }

  render() {
    const {groups, ...popProps} = this.props; // eslint-disable-line no-unused-vars
    return (
      <Popover {...popProps} id="other-pop">
        <div>
          <ul className="mdw-legend">
            {
              groups.map((group, i) => {
                return (
                  <li key={i}>
                      {group.name}
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

export default OtherPop;

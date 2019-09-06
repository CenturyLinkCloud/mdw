import React, {Component} from '../node/node_modules/react';
import {Popover} from '../node/node_modules/react-bootstrap';
import MdwContext from '../react/MdwContext';
import HeaderPopButton from '../react/HeaderPopButton.jsx';

class StagesPopButton extends Component {

  constructor(...args) {
    super(...args);
    this.handleSelect = this.handleSelect.bind(this);
    this.state = { };
  }

  handleSelect() {
    this.refs.stagesPopRef.hide();
  }

  componentDidUpdate() {
    if (this.context.authUser.cuid && !this.state.stages) {
      var ok = false;
      fetch(new Request(this.context.serviceRoot + '/com/centurylink/mdw/staging', {
        method: 'GET',
        headers: { Accept: 'application/json', 'mdw-app-id': 'mdw-hub' },
        credentials: 'same-origin'
      }))
      .then(response => {
        ok = response.ok;
        return response.json();
      })
      .then(json => {
        this.setState({stages: ok ? json : []});
      });
    }
  }  

  render() {
    const {...popProps} = this.props; // eslint-disable-line no-unused-vars
    if (this.state.stages) {
      return (
        <HeaderPopButton ref="stagesPopRef"
          glyph="menu-hamburger"
          title="Staging Areas"
          rootClose={true}
          popover={
            <Popover {...popProps} id="stages-pop">
              <ul className="dropdown-menu mdw-popover-menu">
                {
                  this.state.stages.map((stage, i) => {
                    return (
                      <li key={i}>
                        <a href={this.context.hubRoot + '/#/staging/' + stage.userCuid}
                          onClick={() => this.handleSelect(stage.userCuid)}>
                          {stage.userName}
                        </a>
                      </li>
                    );
                  })
                }
              </ul>
            </Popover>
          } 
        />
      );
    }
    else {
      return <span></span>;
    }
  }
}

StagesPopButton.contextType = MdwContext;
export default StagesPopButton;

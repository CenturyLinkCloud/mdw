import React from '../../node/node_modules/react';
import PropTypes from '../../node/node_modules/prop-types';
import {Popover, OverlayTrigger, Button, Glyphicon} from '../../node/node_modules/react-bootstrap';
import '../../node/node_modules/style-loader!./filepanel.css';

function Toolbar(props, context) {

  var options = localStorage.getItem('filepanel-options');
  if (options) {
    options = JSON.parse(options);
  }
  else {
    options = {};
  }
  
  const handleChange = event => {
    if (event.currentTarget.name === 'lineNumbers') {
      options.lineNumbers = event.currentTarget.checked;
      props.onOptions(options);
    }
  };
  
  const optionsPopover = (
    <Popover id="options-pop">
      <div className="fp-options">
        <label>
          <input name="lineNumbers" type="checkbox" onChange={handleChange} checked={options.lineNumbers} />
            Line Numbers
          </label>
      </div>
    </Popover>
  );
  
  return (
    <div>
      <div>
        <OverlayTrigger trigger="click" placement="right" overlay={optionsPopover} rootClose={true}>
          <Button>Options</Button>
        </OverlayTrigger>          
      </div>
    </div>
  );
}
  
Toolbar.contextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string
};

export default Toolbar;
import React from '../../node/node_modules/react';
import PropTypes from '../../node/node_modules/prop-types';
import {Popover, OverlayTrigger, Button, Glyphicon} from '../../node/node_modules/react-bootstrap';
import Search from './Search.jsx';
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
  
  const isFile = props.item && props.item.isFile;
  return (
    <div className="fp-toolbar">
      <div style={{display:'flex', alignItems:'center', float:'left'}}>
        <div>
          <OverlayTrigger trigger="click" placement="right" overlay={optionsPopover} rootClose={true}>
            <Button style={{marginRight:'20px'}}>Options</Button>
          </OverlayTrigger>
        </div>
        <div>
          {isFile &&
            <div >
              <Search />
              <div style={{display:'flex'}}>
                {!props.item.binary &&
                  <Button className="fp-icon-btn">
                    <Glyphicon glyph="refresh" />
                  </Button>
                }
                <Button className="fp-icon-btn">
                 <Glyphicon glyph="download-alt" />
                </Button>
                {!props.item.binary &&
                  <div>
                    <label>
                      <input name="tailMode" type="checkbox" onChange={handleChange} checked={options.tailMode} />
                      Tail Mode
                    </label>
                  </div>
                }
              </div>
            </div>
          }
        </div>
      </div>
      {isFile && !props.item.binary && props.item.lineCount &&
        <div style={{float:'right', display:'flex', alignItems:'center'}}>
          <div className="fp-line-info">
            {props.line + ' / ' + props.item.lineCount}
          </div>
          <Button className="fp-icon-btn">
            <Glyphicon glyph="step-forward" style={{transform:'rotate(90deg)'}}/>
          </Button>
        </div>
      }
    </div>
  );
}
  
Toolbar.contextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string
};

export default Toolbar;
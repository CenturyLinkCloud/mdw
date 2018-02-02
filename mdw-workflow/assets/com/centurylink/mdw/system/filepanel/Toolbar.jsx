import React from '../../node/node_modules/react';
import PropTypes from '../../node/node_modules/prop-types';
import {Popover, OverlayTrigger, Button, Glyphicon} from '../../node/node_modules/react-bootstrap';
import Search from './Search.jsx';
import '../../node/node_modules/style-loader!./filepanel.css';

Toolbar.BUFFER_SIZE = 500;
Toolbar.FETCH_THRESHOLD = 200;
Toolbar.SEARCH_MIN_LENGTH = 3;

Toolbar.getOptions = () => {
  var options = {};
  let optsStr = localStorage.getItem('filepanel-options');
  if (optsStr) {
    options = JSON.parse(optsStr);
  }
  // default options
  if (!options.bufferSize) {
    options.bufferSize = Toolbar.BUFFER_SIZE;
  }
  if (!options.fetchThreshold) {
    options.fetchThreshold = Toolbar.FETCH_THRESHOLD;
  }
  if (!options.searchMinLength) {
    options.searchMinLength = Toolbar.SEARCH_MIN_LENGTH;
  }
  return options;
};

function Toolbar(props) {

  var options = Toolbar.getOptions();
  
  const handleChange = event => {
    if (event.currentTarget.name === 'tailMode') {
      props.onAction('tailMode');
    }
    else {
      if (event.currentTarget.name === 'lineNumbers') {
        options.lineNumbers = event.currentTarget.checked;
      }
      else if (event.currentTarget.name === 'bufferSize') {
        options.bufferSize = event.currentTarget.value;
      }
      else if (event.currentTarget.name === 'fetchThreshold') {
        options.fetchThreshold = event.currentTarget.value;
      }
      else if (event.currentTarget.name === 'searchWhileTyping') {
        options.searchWhileTyping = event.currentTarget.checked;
      }
      else if (event.currentTarget.name === 'searchMinLength') {
        options.searchMinLength = event.currentTarget.value;
      }
      localStorage.setItem('filepanel-options', JSON.stringify(options));
      props.onOptions(options);
    }
  };
  
  const handleClick = event => {
    props.onAction(event.currentTarget.name);
  }
  
  const optionsPopover = (
    <Popover id="options-pop">
      <div className="fp-options">
        <div>
          <label>
            <input name="lineNumbers" 
              type="checkbox" 
              checked={options.lineNumbers} 
              onChange={handleChange} />
            Line Numbers
          </label>
        </div>
        <div>
          <label>
            Buffer Size:
            <input name="bufferSize" 
              type="number" 
              step="100" min="200" max="1000" 
              value={options.bufferSize} 
              onChange={handleChange} />
          </label>
        </div>
        <div>
          <label>
            Fetch Threshold:
            <input name="fetchThreshold" 
              type="number" 
              step="10" min="100" max="500" 
              value={options.fetchThreshold}
              onChange={handleChange} />
          </label>
        </div>
        <div>
          <label>
            <input name="searchWhileTyping" 
              type="checkbox" 
              checked={Toolbar.getOptions().searchWhileTyping} 
              onChange={handleChange} />
            Match While Typing
          </label>
        </div>
        <div>
          <label style={{paddingLeft:'5px'}}>
            Minimum Characters:
            <input name="searchMinLength" 
              type="number" 
              step="1" min="1" max="10"
              disabled={!Toolbar.getOptions().searchWhileTyping}
              value={Toolbar.getOptions().searchWhileTyping ? Toolbar.getOptions().searchMinLength : ''}
              onChange={handleChange} />
          </label>
        </div>
      </div>
    </Popover>
  );
  
  const isFile = props.item && props.item.isFile;
  return (
    <div className="fp-toolbar">
      <div style={{display:'flex', alignItems:'center', float:'left'}}>
        <div>
          <OverlayTrigger trigger="click" 
            placement="right" 
            overlay={optionsPopover} 
            rootClose={true}>
            <Button>Options</Button>
          </OverlayTrigger>
        </div>
        <div>
          {isFile &&
            <div style={{display:'flex'}}>
              {!props.item.binary &&
                <Search 
                  options={Toolbar.getOptions()} 
                  onAction={props.onAction} 
                  message={props.searchMessage} />
              }
              <div style={{paddingTop:'3px'}}>
                {!props.item.binary &&
                  <Button name="refresh" 
                    className="fp-icon-btn" 
                    title="Refresh" 
                    onClick={handleClick}>
                    <Glyphicon glyph="refresh" />
                  </Button>
                }
                <Button name="download" 
                  className="fp-icon-btn" 
                  title="Download" 
                  onClick={handleClick}>
                  <Glyphicon glyph="download-alt" />
                </Button>
                {!props.item.binary && !props.item.host &&
                  <span>
                    <label>
                      <input name="tailMode" 
                        type="checkbox" 
                        checked={props.tailMode}
                        onChange={handleChange} />
                      Tail Mode
                    </label>
                  </span>
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
          <Button name="scrollToEnd" 
            className="fp-icon-btn" 
            style={{marginTop:'2px'}} 
            title="Scroll to End" 
            onClick={handleClick}>
            <Glyphicon glyph="step-forward" style={{transform:'rotate(90deg)'}}/>
          </Button>
        </div>
      }
    </div>
  );
}
  
export default Toolbar;
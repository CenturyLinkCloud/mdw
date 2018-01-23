import React from '../../node/node_modules/react';
import {Button, Glyphicon} from '../../node/node_modules/react-bootstrap';
import '../../node/node_modules/style-loader!./filepanel.css';

function Search(props) {

  const handleClick = event => {
    props.onAction(event.currentTarget.name);
  }
  
  return (
    <div className="fp-search">
      <div>
        <input type="text" placeholder="Search" />
        <Button 
          className="fp-icon-btn"
          style={{marginLeft:'0'}}
          name="searchBackward" 
          title="Backward" 
          onClick={handleClick}>
          <Glyphicon glyph="chevron-up" />
        </Button>
        <Button 
          className="fp-icon-btn"
          style={{marginLeft:'3px'}}
          name="searchForward" 
          title="Forward" 
          onClick={handleClick}>
          <Glyphicon glyph="chevron-down" />
        </Button>
      </div>
    </div>
  );
}

export default Search;  
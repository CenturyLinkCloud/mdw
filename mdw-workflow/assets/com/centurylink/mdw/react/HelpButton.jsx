import React from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import {Glyphicon} from '../node/node_modules/react-bootstrap';

function HelpButton(props) { // eslint-disable-line no-unused-vars

  return (
    <a className="btn btn-primary" style={{padding:'3px 6px'}}
      href={props.url} 
      title={props.title ? props.title : 'Help'}>
      <Glyphicon glyph="question-sign" 
        style={{fontSize:'18px',position:'relative',top:'2px'}} />
    </a>
  );
}

HelpButton.propTypes = {
  url: PropTypes.string.isRequired,
  title: PropTypes.string
};

export default HelpButton;

import React from '../node/node_modules/react';
import {
  Button,
  FormGroup,
  ControlLabel,
  FormControl,
  HelpBlock,
  Glyphicon
} from '../node/node_modules/react-bootstrap';


function Field({help, ...props}) {
  
  var fieldValidity;
  var status;
  if (props.validity && props.validity[props.id]) {
    fieldValidity = props.validity[props.id];
    status = fieldValidity.status;
  }
  
  console.log("Status: " + status);
  
  return (
      <FormGroup controlId={props.id}
        validationState={status}>
        {props.label && 
          <ControlLabel>{props.label}</ControlLabel>
        }
        <FormControl {...props} />
        <FormControl.Feedback>
          {status === 'error' &&
            <Glyphicon glyph="warning-sign" />
          }
        </FormControl.Feedback>
        {props.help && 
          <HelpBlock>{props.help}</HelpBlock>
        }
      </FormGroup>       
  );
}

export default Field;  
import React from '../node/node_modules/react';
import {
  Button,
  FormGroup,
  ControlLabel,
  FormControl,
  HelpBlock,
  Glyphicon
} from '../node/node_modules/react-bootstrap';

/**
 * validity can be a map like so:
 * {
 *   propId: { status: 'error', message: 'i need help' }
 * }
 * or for this specific field:
 * {
 *   status: 'error',
 *   message: 'back to the drawing board'
 * }
 */
function Field({label, help, validity, ...props}) {

  if (validity) {
    if (validity[props.id]) {
      validity = validity[props.id];
    }
  }
  else {
    validity = {};
  }
  
  return (
    <FormGroup controlId={props.id}
      validationState={validity.status}>
      {label && 
        <ControlLabel style={{marginBottom:'3px'}}>
          {label}
        </ControlLabel>
      }
      <FormControl {...props} />
      <FormControl.Feedback>
        {validity.status === 'error' &&
          <Glyphicon glyph="warning-sign"
            style={{fontSize:'19px'}}/>
        }
      </FormControl.Feedback>
      {validity.message && 
        <HelpBlock style={{position:'absolute',color:'#a94442',marginTop:'0',marginLeft:'5px'}}>
          {validity.message}
        </HelpBlock>
      }
      {help && 
        <HelpBlock style={{visibility:validity.message ? 'hidden' : 'visible'}}>
          {help}
        </HelpBlock>
      }
    </FormGroup>       
  );
}

export default Field;
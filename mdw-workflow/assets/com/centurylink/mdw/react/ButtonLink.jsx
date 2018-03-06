import React from '../node/node_modules/react';
import {Button, Glyphicon} from '../node/node_modules/react-bootstrap';
import {Link} from '../node/node_modules/react-router-dom';

function ButtonLink({linkTo, glyph, ...props}) {
  
  if (props.className)
    props.className = 'btn ' + props.className;
  else
    props.className = 'btn btn-primary';
  
  return (
      <Link to={linkTo} {...props}>
        {glyph &&
          <Glyphicon glyph={glyph} />
        }
        {props.children}
      </Link>
  );
}

export default ButtonLink; 
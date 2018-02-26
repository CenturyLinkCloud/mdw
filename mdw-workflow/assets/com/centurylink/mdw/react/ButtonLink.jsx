import React from '../node/node_modules/react';
import {Button, Glyphicon} from '../node/node_modules/react-bootstrap';
import {Link} from '../node/node_modules/react-router-dom';

function ButtonLink({linkTo, glyph, ...props}) {
  
  return (
    <Button {...props}>
      <Link to={linkTo}>
        {glyph &&
          <Glyphicon glyph={glyph} />
        }
      </Link>
    </Button>
  );
}

export default ButtonLink; 
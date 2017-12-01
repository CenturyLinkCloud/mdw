import React from '../node/node_modules/react';
import {Button, Glyphicon} from '../node/node_modules/react-bootstrap';
import UserDate from './UserDate.jsx';

function Comment(props) {
  
  return (
    <div key={props.comment.id} className="panel panel-default mdw-panel">
      <div className="panel-heading mdw-heading">
          <div className="mdw-heading-label">
          {!props.comment.id &&
            <span>Add a comment</span>
          }
          {props.comment.id && !props.comment.modifyUser &&
            <span style={{fontWeight:'normal'}}>
              Created {' '}
              <UserDate date={props.comment.created} />
              {' by '} <a href="">{props.comment.createUser}</a>
            </span>
          }
          {props.comment.modifyUser &&
            <span style={{fontWeight:'normal',marginLeft:'10px'}}>
              Modified {' '}
              <UserDate date={props.comment.modified} />
              {' by '} <a href="">{props.comment.modifyUser}</a>
            </span>
          }
        </div>
        <div className="mdw-heading-actions">
          {props.comment.editable &&
            <span>
              <Button className="mdw-btn mdw-action-btn" bsStyle='success'
                onClick={() => props.actionHandler('save', props.comment)}>
                Save
              </Button>
              <Button className="mdw-btn mdw-action-btn mdw-cancel-btn"
                onClick={() => props.actionHandler('cancel', props.comment)}>
                Cancel
              </Button>
            </span>
          }
          {!props.comment.editable &&
            <Button className="mdw-btn mdw-action-btn"
              onClick={() => props.actionHandler('edit', props.comment)}>
              <Glyphicon glyph="pencil" />
            </Button>
          }
        </div>
      </div>
      {props.comment.editable &&
        <textarea className="mdw-section" style={{width:'100%'}} 
          value={props.comment.content} 
          onChange={event => props.actionHandler('update', props.comment, event.currentTarget.value)}/>
      }
      {!props.comment.editable &&
        <div className="mdw-section">
          {props.comment.content}
        </div>
      }
    </div>
  );
}

export default Comment;
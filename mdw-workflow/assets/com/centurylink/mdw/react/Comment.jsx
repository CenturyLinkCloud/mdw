import React from '../node/node_modules/react';
import {Button, Glyphicon, Popover, OverlayTrigger} from '../node/node_modules/react-bootstrap';
import ReactMarkdown from '../node/node_modules/react-markdown';
import {Emoji, Picker} from '../node/node_modules/emoji-mart';
import '../node/node_modules/style-loader!./emoji-mart.css';

import UserDate from './UserDate.jsx';

function Comment(props) {
  
  const getSegments = content => {
    const segments = [];
    const re = /:[^\s]+?:/g;
    var match;
    var last = 0;
    while ((match = re.exec(content)) != null) {
      if (match.index > last) {
        segments.push({text: content.substring(last, match.index)});
      }
      segments.push({emoji: match[0]});
      last = re.lastIndex;
    }
    if (content && (last < content.length)) {
      segments.push({text: content.substring(last)});
    }
    
    if (segments.length === 0) {
      segments.push({text: content});
    }
    
    return segments;
  };
  
  let overlayRef;
  
  const insertEmoji = emoji => {
    const ta = document.getElementById('comment-' + props.comment.id + '-textarea');
    var val = ta.value;
    if (ta.selectionStart || ta.selectionStart == '0') {
      var start = ta.selectionStart;
      var end = ta.selectionEnd;
      val = val.substring(0, start) + emoji.colons + val.substring(end, val.length);
    } 
    else {
      val += emoji.colons;
    }
    props.actionHandler('update', props.comment, val);
    overlayRef.hide();
    ta.focus();
  };
  
  const emojiPopover = (
    <Popover id='emoji-pop'>
      <div>
        <Picker onClick={insertEmoji} title="Emojis" emojiSize={22}
          emojiTooltip={true} />
      </div>
    </Popover>
  );

  var height = 80;
  const section = document.getElementById('comment-' + props.comment.id + '-section');
  if (section) {
    height = section.offsetHeight;
  }
  else {
    const ta = document.getElementById('comment-' + props.comment.id + '-textarea');
    if (ta) {
      height = ta.scrollHeight;
    }
  }
  if (height < 80) {
    height = 80;
  }
  
  return (      
    <div key={props.comment.id} className="panel panel-default mdw-panel mdw-comment-panel">
      <div className="panel-heading mdw-heading">
          <div className="mdw-heading-label">
          {!props.comment.id &&
            <span>Add a comment</span>
          }
          {props.comment.name && props.comment.name.startsWith('slack') &&
            <a href="https://slack.com">
              <img src="../../asset/com/centurylink/mdw/slack/slack-hash.png" alt="slack"
                width={22} height={22} style={{marginRight:'8px'}}/>
            </a>
          }
          {props.comment.id && !props.comment.modifyUser &&
            <span style={{fontWeight:'normal'}}>
              Created {' '}
              <UserDate date={props.comment.created} />
              {' by '} <a href="">{props.comment.createUser}</a>
            </span>
          }
          {props.comment.modifyUser &&
            <span style={{fontWeight:'normal'}}>
              Modified {' '}
              <UserDate date={props.comment.modified} />
              {' by '} <a href="">{props.comment.modifyUser}</a>
            </span>
          }
        </div>
        <div className="mdw-heading-actions">
          {props.comment.editing &&
            <span>
              <Button className="mdw-btn mdw-action-btn" bsStyle='success'
                onClick={() => props.actionHandler('save', props.comment)}>
                Save
              </Button>
              <Button className="mdw-btn mdw-action-btn mdw-cancel-btn"
                onClick={() => props.actionHandler('cancel', props.comment)}>
                Cancel
              </Button>
              <OverlayTrigger trigger="click" placement="left" overlay={emojiPopover} 
                rootClose={true} ref={(ol) => { overlayRef = ol; }}>
                <Button name="emoji" className="mdw-btn mdw-action-btn" 
                  style={{paddingBottom: "0"}} title="Emoji">
                  <Emoji emoji=":slightly_smiling_face:" size="18px" />
                </Button>
              </OverlayTrigger>
            </span>
          }
          {!props.comment.editing && props.editable &&
            <span>
              <Button className="mdw-btn mdw-action-btn"
                onClick={() => props.actionHandler('edit', props.comment)}>
                <Glyphicon glyph="pencil" />
              </Button>
              <Button className="mdw-btn mdw-action-btn"
                onClick={() => props.actionHandler('delete', props.comment)}>
                <Glyphicon glyph="remove" />
              </Button>
            </span>
          }
        </div>
      </div>
      {props.comment.editing &&
        <textarea id={'comment-' + props.comment.id + '-textarea'} autoFocus
          className="mdw-section" style={{height:height + 'px'}} value={props.comment.content}
          onChange={event => props.actionHandler('update', props.comment, event.currentTarget.value)} />
      }
      {!props.comment.editing &&
        <div id={'comment-' + props.comment.id + '-section'} className="mdw-section">
          {
            getSegments(props.comment.content).map((segment, i) => {
              return (
                <span key={i} className={segment.emoji ? 'mdw-emoji' : ''} 
                  title={segment.emoji ? segment.emoji : null}>
                {segment.emoji &&
                  <Emoji emoji={segment.emoji} size="22px" />
                }
                {segment.text &&
                  <ReactMarkdown source={segment.text} className="mdw-markdown-segment" />
                }
                </span>
              );
            })
          }
        </div>
      }
    </div>
  );
}

export default Comment;
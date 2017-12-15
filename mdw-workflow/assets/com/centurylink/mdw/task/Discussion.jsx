import React, {Component} from '../node/node_modules/react';
import {Button, Glyphicon} from '../node/node_modules/react-bootstrap';
import Comment from '../react/Comment.jsx';
import Heading from './Heading.jsx';

class Discussion extends Component {
    
  constructor(...args) {
    super(...args);
    this.state = { comments: [] };
    this.handleAction = this.handleAction.bind(this);
    this.addNewComment = this.addNewComment.bind(this);
    this.saveComment= this.saveComment.bind(this);
  }
  
  componentDidMount() {
    fetch(new Request('/mdw/services/Comments?ownerType=TASK_INSTANCE&ownerId=' + this.props.task.id, {
      method: 'GET',
      headers: { Accept: 'application/json'},
      credentials: 'same-origin'
    }))
    .then(response => {
      return response.json();
    })
    .then(comments => {
      this.setState({
        comments: comments
      });
    });
  }
  
  handleAction(action, comment, content) {
    if (action === 'save') {
      this.saveComment(comment);
    }
    else if (action === 'delete') {
      this.deleteComment(comment);
    }
    else {
      const comments = this.state.comments.slice(0);
      if (action === 'edit') {
        comment.editing = true;
      }
      else if (action === 'cancel') {
        // id present means existing rather than new
        if (comment.id) {
          if (comment.originalContent) {
            comment.content = comment.originalContent;
            delete comment.originalContent;
          }
        }
        else {
          comments.splice(0, 1);
        }
        comment.editing = false;
      }
      else if (action === 'update') {
        if (comment.id && !comment.originalContent) {
          comment.originalContent = comment.content;
        }
        comment.content = content;
        const index = comment.id ? comments.findIndex(cmt => {
          return cmt.id === comment.id;
        }) : comments.length - 1;
        comments[index] = comment;
      }
      this.setState({ comments: comments });
    }
  }
  
  addNewComment() {
    const comments = this.state.comments.slice(0);
    comments.push({
      created: new Date().toISOString(),
      content: this.state.content,
      editing: true,
      editable: true
    });
    this.setState({comments: comments});
  }
  
  saveComment(comment) {
    // remove temp values
    delete comment.editing;
    delete comment.editable;
    delete comment.originalContent;
    comment.ownerType = 'TASK_INSTANCE';
    comment.ownerId = this.props.task.id;
    
    var url = '/mdw/services/Comments';
    if (comment.id) {
      // update existing (PUT)
      url += '/' + comment.id;
      comment.modifyUser = $mdwUi.authUser.cuid,
      comment.modified = new Date().toISOString();
    }
    else {
      // create new (POST)
      comment.createUser = $mdwUi.authUser.cuid,
      comment.created = new Date().toISOString();
    }
    var ok = false;
    fetch(new Request(url, {
      method: comment.id ? 'PUT' : 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(comment),
      credentials: 'same-origin'
    }))
    .then(response => {
      ok = response.ok;
      return response.json();
    })
    .then(json => {
      if (ok) {
        if (!comment.id) {
          comment.id = json.id;
        }
        this.setState({
          comments: this.state.comments
        });
      }
      else {
        $mdwUi.showMessage(json.status.message);
      }
    });
  }

  deleteComment(comment) {
    var url = '/mdw/services/Comments/' + comment.id;
    var ok = false;
    fetch(new Request(url, {
      method: 'DELETE',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'same-origin'
    }))
    .then(response => {
      ok = response.ok;
      return response.json();
    })
    .then(json => {
      if (ok) {
        const comments = this.state.comments.slice(0);
        const index = comments.findIndex(cmt => {
          return cmt.id === comment.id;
        });
        comments.splice(index, 1);
        this.setState({
          comments: comments
        });
      }
      else {
        $mdwUi.showMessage(json.status.message);
      }
    });
  }
  
  render() {
    return (
      <div>
        <Heading task={this.props.task} refreshTask={this.props.refreshTask}>
          <Button className="mdw-btn mdw-action-btn" bsStyle='primary' 
            onClick={this.addNewComment}>
            <Glyphicon glyph="plus" />{' New'}
          </Button>
        </Heading>
        <div className="mdw-section">
          {
            this.state.comments.map(comment => {
              return (
                <Comment key={comment.id ? comment.id : 0} comment={comment} editing={false}
                  editable={comment.createUser === $mdwUi.authUser.name || comment.createUser === $mdwUi.authUser.cuid}
                  actionHandler={this.handleAction} changeHandler={this.handleChange} />
              );
            })
          }
        </div>
      </div>
    );
  }
}

export default Discussion;
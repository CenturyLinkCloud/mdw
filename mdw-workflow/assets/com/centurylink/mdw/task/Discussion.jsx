import React, {Component} from '../node/node_modules/react';
import {Button, Glyphicon} from '../node/node_modules/react-bootstrap';
import Heading from './Heading.jsx';
import UserDate from '../react/UserDate.jsx';
import values from '../react/values';


class Discussion extends Component {
    
  constructor(...args) {
    super(...args);
    this.state = { 
      comments: [],
      childVisible: false,
      addNewComment: false,
      content: ''
    };
    
    this.renderComment = this.renderComment.bind(this);  
    this.handleChange = this.handleChange.bind(this);
    this.saveComment=this.saveComment.bind(this);
  }
  
  componentDidMount() {
    // TODO fetch discussion comments from service
    fetch(new Request('/mdw/services/Tasks/' + this.props.task.id + '/comments', {
          method: 'GET',
          headers: { Accept: 'application/json'}
        }))
        .then(response => {
          return response.json();
        })
        .then(vals => {
          this.setState({
              comments: values.toArray(vals)
          });
        });
    /* }
    this.setState({
      comments: [{
          id: 123456,
          content: 'Now is the time for all good men to come to the aid of their country.',
          created: new Date(new Date().getTime() - 24*60*60*1000),
          createUser: 'Donald Oakes'
        }, {
          id: 234567,
          content: 'This comment has been modified.',
          created: new Date(new Date().getTime() - 12*60*60*1000),
          createUser: 'Manoj Agrawal',
          modified: new Date(new Date().getTime() - 4*60*60*1000),
          modifyUser: 'Vimala Gubbi Nagaraja'
        }
      ]
    });*/
  }
  
  renderComment(comment){
      return (
              <div key={comment.id} className="panel panel-default mdw-panel">
                <div className="panel-heading mdw-heading">
                    <div className="mdw-heading-label">
                    <span style={{fontWeight:'normal'}}>
                      created {' '}
                      <UserDate date={comment.created} />
                      {' by '} <a href="">{comment.createUser}</a>
                    </span>
                    {comment.modifyUser &&
                      <span style={{fontWeight:'normal',marginLeft:'10px'}}>
                        (modified {' '}
                        <UserDate date={comment.modified} />
                        {' by '} <a href="">{comment.modifyUser})</a>
                      </span>
                    }
                  </div>
                  <div className="mdw-heading-actions">
                    <Button className="mdw-btn mdw-action-btn" 
                    onClick={() => this.toggleState()} bsStyle='primary'>
                      <Glyphicon glyph="pencil" />
                    </Button>
                  </div>
                </div>
                {this.state.childVisible?   
                 <div className="mdw-section">
                  {'Yes'}
                 </div>:
                 <div className="mdw-section">
                  {comment.content}
                 </div>
              
                }              
                </div>
          );
  }
  
  renderComments (comments) {
      return [comments.map(this.renderComment)];
    }
  
  handleChange(event){
       this.setState({content: event.target.value});     
  }

  addNewComment(){
      this.setState({addNewComment: !this.state.addNewComment});
  }
  
  saveComment(){
       var note={
       createUser: $mdwUi.authUser.cuid,
       taskInstanceId: this.props.task.id,    
       content: this.state.content                   
      };
      if(note.content === null || note.content.trim() === '') {
          return;
      }
      fetch( new Request( '/mdw/services/Tasks/' + this.props.task.id + '/comments', {
          method: 'POST',
          headers: { Accept: 'application/json' },
          body: JSON.stringify(note)
      } ) )
          .then( response => {
             return response.json();
          } )
          .then(vals => {
              this.setState({
                  comments: values.toArray(vals),
                  childVisible: false,
                  addNewComment: false,
                  content: ''
              });
            });
      
  }

  render() {
    return (
      <div>
        <form name="discussionForm" className="form-horizontal" role="form">    
          <Heading task={this.props.task} refreshTask={this.props.refreshTask}>
            <Button className="mdw-btn mdw-action-btn" bsStyle='primary' onClick={() => this.addNewComment()}>
            <Glyphicon glyph="plus" />{' New'}
            </Button>
          </Heading>
         <div className="mdw-section">
            {this.state.addNewComment && <div className="mdw-section">  
                <textarea className="mdw-section" onChange={this.handleChange}
                   name= "comment" value={this.state.content} style={{ width: '100%' }} />
                </div>}
                
                {this.state.addNewComment && <div className="mdw-heading-actions">  
                    <Button className="mdw-btn mdw-action-btn" bsStyle='primary' onClick={this.saveComment}>
                     {'Save'}
                  </Button> 
                  <Button className="mdw-btn mdw-action-btn" bsStyle='primary' onClick={() => this.addNewComment()} >
                     {' Cancel'}
                  </Button>           
                </div>}               
            {this.renderComments(this.state.comments)}     
         </div>
        </form>    
      </div>
    );
  }
}

export default Discussion;
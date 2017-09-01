import React, {Component} from '../node/node_modules/react';
import {Table} from '../node/node_modules/react-bootstrap';
import Subtask from '../react/Subtask.jsx';
import {Button, Glyphicon} from '../node/node_modules/react-bootstrap';
import {Link} from '../node/node_modules/react-router-dom';


class Subtasks extends Component {
    
  constructor(...args) {
    super(...args);
    this.state = { subtasks: [] };
  }  

  componentDidMount() {
    fetch(new Request('/mdw/services/Tasks/' + this.props.task.id + '/subtasks', {
      method: 'GET',
      headers: { Accept: 'application/json'}
    }))
    .then(response => {
      return response.json();
    })
    .then(vals => {
      return vals['subtasks'];
    })
    .then(vals => {
      var subtasks = [];
      Object.keys(vals).forEach(key => {
              var val = vals[key];
              subtasks.push(val);
    
      });
      subtasks.sort((val1, val2) => {
        var diff = val1.id - val2.id;
        if (diff === 0) {
          var label1 = val1.name;
          var label2 = val2.name;
          return label1.toLowerCase().localeCompare(label2.toLowerCase());
        }
        else {
          return diff;
        }
      });      
      this.setState({
        subtasks: subtasks
      });
    });    
  }
  
  render() {
      debugger;
      var rows = [];
      this.state.subtasks.map((subtask) => {   
          rows.push(<Row subtask = {subtask} key = {subtask.id} />);
        });
        return (<div>
          <div>
            <label className = "col-xs-2">Name</label>
            <label className = "col-xs-2">Master Request ID</label>
            <label className = "col-xs-2">Task ID</label>
            <label className = "col-xs-2">Due Date</label>
            <label className = "col-xs-2">Start Date</label>
            <label >Status</label>
         </div>
        {rows}
            <div className="panel-heading mdw-heading">
                <div className="mdw-heading-label">
                    <div className="form-group">
                    <label className="control-label col-xs-2" />
                    <div className="col-xs-4">
                      <Button className="mdw-btn" bsStyle='primary'>
                        <Glyphicon glyph="plus" />{' New'}
                      </Button>
                    </div>
                  </div>
                 </div>
             </div>
        </div>);
      }
    }

function Row(props) {
  return (
      <div>
        <div className = "col-xs-2">{props.subtask.name}</div> 
        <Link to={$mdwHubRoot + '/#/workflow/masterRequests/' + props.subtask.masterRequestId} className="col-xs-2">
            {props.subtask.masterRequestId}
        </Link>     
        <div className = "col-xs-2"><a href={'/#/tasks/' + props.subtask.id} 
        className="mdw-link">{props.subtask.id}</a></div>
        <div className = "col-xs-2">{props.subtask.due}</div>
        <div className = "col-xs-2">{props.subtask.start}</div>
        <div >{props.subtask.status}</div>
      </div>
  );
}

export default Subtasks;  
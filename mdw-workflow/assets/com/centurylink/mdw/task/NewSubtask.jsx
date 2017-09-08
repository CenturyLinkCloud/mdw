import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import {Button, Glyphicon} from '../node/node_modules/react-bootstrap';
import {AsyncTypeahead, Menu, MenuItem} from '../node/node_modules/react-bootstrap-typeahead';
import '../node/node_modules/style-loader!../react/typeahead.css';
var classNames = require('../node/node_modules/classnames');



class NewSubtask extends Component {
    
  constructor(...args) {
    super(...args);
    this.state = { templates: [] ,
                   filteredTemplateList : [] ,
                   template: ""
                 };
    this.handleCancel = this.handleCancel.bind(this);
    this.handleCreate = this.handleCreate.bind(this);    
    this.findTemplate = this.findTemplate.bind(this);
    this.handleTemplateSelection = this.handleTemplateSelection.bind(this);
  }  

  componentDidMount() {
    fetch(new Request('/mdw/services/Tasks/templates?app=mdw-admin', {
      method: 'GET',
      headers: { Accept: 'application/json'}
    }))
    .then(response => {
      return response.json();
    })
    .then(json => {
        this.setState({
            templates: json,
            filteredTemplateList: json.map(temps => {
              return { name: temps.name, label: temps.name };
            }) 
        });
      });
    }

  handleCreate(event) {
    if (this.state.template === "") {
        $mdwUi.showMessage('Please choose a template!');
        return;
    }
    console.log('creating subtask: ' +this.state.template[0].logicalId);
    var ok = false;  
    var createAction = {
            taskAction: 'create',
            logicalId: this.state.template[0].logicalId,
            user: $mdwUi.authUser.cuid,
            masterTaskInstanceId: this.props.task.id
    };

    fetch(new Request(this.context.serviceRoot + '/Tasks/create', {
      method: 'POST',
      headers: { Accept: 'application/json'},
      body: JSON.stringify(createAction)
    }))
    .then(response => {
      ok = response.ok;
      return response.json();
    })
    .then(json => {
      if (ok) {
        $mdwUi.clearMessage();
      }
      else {
        $mdwUi.showMessage(json.status.message);
      }
    });
    window.location.assign('/mdw/tasks/' + this.props.task.id +'/subtasks');
  }
  
  handleCancel(event) {
      window.location.assign('/mdw/tasks/' + this.props.task.id +'/subtasks');
  }
  findTemplate(input) {
      var filteredTemplateList = [];
      var templateName;
      for (var i = 0; i < this.state.templates.length; i++) {
          templateName = this.state.templates[i].name;
          if (templateName.indexOf(input) >= 0) {
              filteredTemplateList.push(this.state.templates[i]);
          }
      }
        this.setState({
            filteredTemplateList: filteredTemplateList
        });
    }



    handleTemplateSelection(input) {
        $mdwUi.clearMessage();
        this.setState({
            template: input
          });
    }
//https://github.com/ericgio/react-bootstrap-typeahead/blob/master/docs/Props.md
  render() {
    return (
      <div>
        <div className="mdw-section">
          <form name="newSubtaskForm" className="form-horizontal" role="form">
            {
                <div className="form-group" >
                  <label className="control-label col-xs-2 mdw-required" > Template:
                  </label>   
                  <div className="col-xs-4" >
                      <AsyncTypeahead ref="typeahead" onSearch={this.findTemplate} autoFocus 
                       options={this.state.filteredTemplateList}  bodyContainer={true}  labelKey="name" 
                       minLength={1} placeholder=' find template' 
                       onChange={this.handleTemplateSelection} />
                 </div>
              </div>
            }
            {
              <div className="form-group">
                <label className="control-label col-xs-2" />
                <div className="col-xs-4">
                  <Button className="btn-success mdw-btn" bsStyle='primary' onClick={this.handleCreate}>
                    <Glyphicon glyph="plus" />{' Create'}
                  </Button>
                  <Button className="mdw-btn mdw-cancel-btn" bsStyle='primary' onClick={this.handleCancel}>
                    <Glyphicon glyph="cancel" />{' Cancel'}
                  </Button>
                </div>
              </div>
            }
          </form>
        </div>
      </div>
    );
  }
}

NewSubtask.contextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string  
};

export default NewSubtask;  
import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import {ButtonToolbar, Glyphicon} from '../node/node_modules/react-bootstrap';
import {AsyncTypeahead} from '../node/node_modules/react-bootstrap-typeahead';
import {Link} from '../node/node_modules/react-router-dom';
import '../node/node_modules/style-loader!../react/typeahead.css';

class NewSubtask extends Component {
    
  constructor(...args) {
    super(...args);
    this.state = { templates: [] ,
                   filteredTemplateList : [] ,
                   template: ""
                 };
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

  handleCreate() {
    if (this.state.template === "") {
        $mdwUi.showMessage('Please choose a template!');
        return;
    }
    console.log('creating subtask: ' + this.state.template[0].logicalId); // eslint-disable-line no-console
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
//https://react-bootstrap.github.io/components.html
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
                <ButtonToolbar>
                  <Link to={this.context.hubRoot + '/tasks/' + this.props.task.id} onClick={this.handleCreate}
                    className="btn mdw-btn btn-primary" style={{fontWeight:'normal',fontSize:'14px'}}>
                    <Glyphicon glyph="plus" />{' Create'}
                  </Link>
                  <Link to={this.context.hubRoot + '/tasks/' + this.props.task.id + '/subtasks'} 
                    className="btn mdw-btn btn-primary" style={{fontWeight:'normal',fontSize:'14px'}}>
                    {' Cancel'}
                  </Link>                  
                </ButtonToolbar>
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
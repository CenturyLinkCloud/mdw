import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import {Button, Modal, ControlLabel} from '../node/node_modules/react-bootstrap';

class Enter extends Component {
  constructor(...args) {
    super(...args);
    this.state = {show: false, message: '', entry: ''};
    this.handleChange = this.handleChange.bind(this);
    this.handleKeyDown = this.handleKeyDown.bind(this);
  }

  close(result) {
    this.setState({show: false, message: '', entry: ''});
    if (this.props.onClose) {
      this.props.onClose(result);
    }
  }

  open(message) {
    this.setState({show: true, message: message});
  }

  handleChange(e) {
    let value = e.currentTarget.value;
    this.setState({show: this.state.show, message: this.state.message, entry: value},
      () => {
        if (this.props.onChange) {
          this.props.onChange(value);
        }
    });
  }

  handleKeyDown(e) {
    if (e.key === 'Enter') {
      e.preventDefault();
      e.stopPropagation();
      this.close(this.state.entry);      
    }    
  }

  render() {
    return (
      <div>
        <Modal show={this.state.show} onHide={() => this.close()}>
          <Modal.Header closeButton 
            style={{paddingBottom:this.props.error || this.props.error === '' ? '25px' : '15px'}}>
            <Modal.Title>
              {this.props.title}
            </Modal.Title>
            {this.props.error &&
              <span className="mdw-warn" style={{position:'absolute'}}>
                {this.props.error}
              </span>
            }
          </Modal.Header>
          <Modal.Body style={{width:'500px'}}>
            <div>
              {this.state.message}
            </div>
            <div>
              {this.props.label &&
                <ControlLabel style={{marginRight:'5px'}}>
                  {this.props.label}
                </ControlLabel>
              }
              <input type="text" style={{width:'419px',marginTop:'10px'}}
                value={this.state.entry} autoFocus
                onChange={this.handleChange}
                onKeyDown={this.handleKeyDown} />
            </div>
          </Modal.Body>
          <Modal.Footer>
            <Button onClick={() => this.close() }>
              Cancel
            </Button>
            <Button bsStyle="primary" onClick={() => this.close(this.state.entry)}>
              OK
            </Button>
          </Modal.Footer>
        </Modal>
      </div>
    );  
  }
}

Enter.propTypes = {
  title: PropTypes.string.isRequired,
  error: PropTypes.string,
  onChange: PropTypes.func,
  onClose: PropTypes.func
};

export default Enter;
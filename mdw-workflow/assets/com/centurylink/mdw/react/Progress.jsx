import React, {Component} from '../node/node_modules/react';
import PropTypes from '../node/node_modules/prop-types';
import {ProgressBar} from '../node/node_modules/react-bootstrap';

class Progress extends Component {
    
  constructor(...args) {
    super(...args);
    this.getProgressComponent = this.getProgressComponent.bind(this);  
    this.state = { progresses: [] };
  }  

  componentDidMount() {
      this.websocket = new WebSocket(this.props.webSocketUrl);
      this.websocket.onopen = () => {
        this.websocket.send(this.props.topic);
        if (this.props.onStart) {
          this.props.onStart();
        }
      };
      this.websocket.onmessage = event => {
        const progress = JSON.parse(event.data);
        if (progress.error) {
          if (this.props.onError) {
            this.props.onError(progress.error);
          }
        }
        else {
          if (this.state.progresses.length === 0 && progress.task) {
            this.state.progresses.push(progress);
          }
          else {
            const progresses = this.state.progresses;
            if (progress.done) {
              progresses.forEach(prog => prog.done = true);
            }
            else if (progress.task && progresses[progresses.length-1].task !== progress.task) {
              progresses[progresses.length-1].done = true;
              progresses.push(progress);
            }
            else {
              progresses[progresses.length-1] = progress;
            }
          }
          this.setState({
            progress: progress
          }, () => {
            if (progress.done && this.props.onFinish) {
              this.props.onFinish();
            }
          });
        }
      };
  }

  componentWillUnmount() {
    if (this.websocket) {
      this.websocket.close();
    }
  }

  getProgressComponent(key, progress) {
    const percent = progress.done ? 100 : (progress.progress ? progress.progress : 0);
    return (
      <div key={key} style={{marginTop:'6px'}}>
        <span style={{marginTop:'3px'}}>
          {(progress.task ? progress.task : ' ') + (progress.done ? '... done' : '...')}
        </span>
        <ProgressBar style={{width:'480px',marginTop:'3px'}}
          active={percent !== 0 && percent != 100} 
          now={percent}
          label={percent + '%'} />        
      </div>
    );
  }

  render() {
    return (
      <div>
        {this.state.progresses.length > 0 &&
          <div>
            <span style={{fontWeight:'bold'}}>
              {this.state.progresses[0].title}:
            </span>
            {
              this.state.progresses.map((progress, i) => {
                return this.getProgressComponent(i, progress);
              })
            }
          </div>
        }
        {this.state.progresses.length === 0 &&
          <div>
            <span style={{fontWeight:'bold'}}>
              {this.props.title || 'In progress'}:
            </span>
            <div>
              {this.getProgressComponent(-1, {progress: 0})}
            </div>
          </div>
        }
      </div>
    );
  }
}

Progress.propTypes = {
  title: PropTypes.string,
  webSocketUrl: PropTypes.string.isRequired,
  topic: PropTypes.string.isRequired,
  onStart: PropTypes.func,
  onError: PropTypes.func,
  onFinish: PropTypes.func
};

export default Progress; 
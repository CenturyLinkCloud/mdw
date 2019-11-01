import React, {Component} from '../node/node_modules/react';
import {Button} from '../node/node_modules/react-bootstrap';
import Dropzone from '../node/node_modules/react-dropzone';
import MdwContext from '../react/MdwContext';
import languages from '../react/languages';
import CodeBlock from '../react/CodeBlock.jsx';
import Workflow from '../react/Workflow.jsx';
import CodeDiff from '../react/CodeDiff.jsx';
import Confirm from '../react/Confirm.jsx';
import AssetHeader from './AssetHeader.jsx';

class StagedAsset extends Component {
    
  constructor(...args) {
    super(...args);

    this.handleUnstage = this.handleUnstage.bind(this);
    this.handleConfirmUnstage = this.handleConfirmUnstage.bind(this);
    this.doUnstage = this.doUnstage.bind(this);
    this.handleDelete = this.handleDelete.bind(this);
    this.handleConfirmDelete = this.handleConfirmDelete.bind(this);
    this.doDelete = this.doDelete.bind(this);
    this.handleViewChange = this.handleViewChange.bind(this);
    this.handleDrop = this.handleDrop.bind(this);
    this.handleFileOpen = this.handleFileOpen.bind(this);
    this.getRenderedMarkdown = this.getRenderedMarkdown.bind(this);

    this.confirmUnstageDialog = React.createRef();
    this.confirmDeleteDialog = React.createRef();
    this.dropzone = React.createRef();

    if (this.props.match && this.props.match.params) {
      this.stagingCuid = this.props.match.params.cuid;
      this.package = this.props.match.params.package;
      this.assetName = this.props.match.params.asset;
    }
    this.state = { 
      view: 'asset',
      asset: undefined, 
      content: undefined };
  }

  componentDidMount() {
    if (this.package && this.assetName) {
      let pathPlusParam = this.package + '/' + this.assetName + '?stagingUser=' + this.stagingCuid;
      let url = this.context.serviceRoot + '/Assets/' + pathPlusParam;
      $mdwUi.clearMessage();
      $mdwUi.hubLoading(true);
      let ok = false;
      fetch(new Request(url, {
        method: 'GET',
        headers: { Accept: 'application/json', 'mdw-app-id': 'mdw-hub' },
        credentials: 'same-origin'
      }))
      .then(response => {
        $mdwUi.hubLoading(false);
        ok = response.ok;
        return response.json();
      })
      .then(json => {
        if (ok) {
          this.setState({asset: json, content: undefined}, () => {
            let asset = json;
            if (!asset.isBinary) {
              $mdwUi.hubLoading(true);
              url = this.context.hubRoot + '/asset/' + pathPlusParam;
              if (asset.name.endsWith('.proc')) {
                url += '&render=json';
              }
              else if (asset.name.endsWith('.md')) {
                url += '&render=html';
              }
              fetch(new Request(url, {
                method: 'GET',
                credentials: 'same-origin'
              }))
              .then(response => {
                $mdwUi.hubLoading(false);
                ok = response.ok || response.status === 404;
                return response.status === 404 ? '' : response.text();
              })
              .then(text => {
                if (ok) {
                  this.setState({asset: asset, content: text, view: this.state.view}, () => {
                    if (asset.vcsDiff) {
                      // retrieve original content
                      $mdwUi.hubLoading(true);
                      url = this.context.hubRoot + '/asset/' + this.package + '/' + this.assetName;
                      fetch(new Request(url, {
                        method: 'GET',
                        credentials: 'same-origin'
                      }))
                      .then(response => {
                        $mdwUi.hubLoading(false);
                        ok = response.ok;
                        return response.text();
                      })
                      .then(text => {
                        this.setState({
                          asset: this.state.asset, 
                          content: this.state.content, 
                          view: this.state.view,
                          oldContent: ok ? text: ''
                        }, () => {
                          if ((asset.name.endsWith('.proc') && !this.state.oldContent.startsWith('{')) || asset.name.endsWith('.md')) {
                            $mdwUi.hubLoading(true);
                            url = this.context.hubRoot + '/asset/' + pathPlusParam;
                            fetch(new Request(url, {
                              method: 'GET',
                              credentials: 'same-origin'
                            }))
                            .then(response => {
                              $mdwUi.hubLoading(false);
                              ok = response.ok;
                              return response.text();
                            })
                            .then(text => {
                              this.setState({
                                asset: this.state.asset, 
                                content: this.state.content, 
                                view: this.state.view,
                                oldContent: this.state.oldContent,
                                newContent: text
                              });
                            });
                          }
                          else {
                            this.setState({
                              asset: this.state.asset, 
                              content: this.state.content, 
                              view: this.state.view,
                              oldContent: this.state.oldContent,
                              newContent: this.state.content
                            });
                          }
                        });
                      });
                    }
                  });
                }
              });
            }
          });
        }
        else {
          $mdwUi.showMessage(json.status.message);
        }
      });
    }
  }

  handleUnstage() {
    if (this.state.asset.vcsDiffType) {
      this.confirmUnstageDialog.current.open('Asset ' + this.state.asset.name + ' has changes.  Unstage?');
    }
    else {
      this.doUnstage();
    }
  }

  handleConfirmUnstage(result) {
    if (result) {
      this.doUnstage(result);
    }
  }

  doUnstage() {
    const url = this.context.serviceRoot + '/com/centurylink/mdw/staging/' + 
        this.stagingCuid + '/assets/' + this.package + '/' + this.assetName;
    $mdwUi.clearMessage();
    $mdwUi.hubLoading(true);
    let ok = false;
    fetch(new Request(url, {
      method: 'DELETE',
      headers: { Accept: 'application/json', 'mdw-app-id': 'mdw-hub' },
      credentials: 'same-origin'
    }))
    .then(response => {
      $mdwUi.hubLoading(false);
      ok = response.ok;
      return response.json();
    })
    .then(json => {
      if (ok) {
        location = '../';
      }
      else {
        $mdwUi.showMessage(json.status.message);
      }
    });    
  }

  handleDelete() {
    this.confirmDeleteDialog.current.open('Delete ' + this.state.asset.name + '?');
  }

  handleConfirmDelete(result) {
    if (result) {
      this.doDelete(result);
    }
  }

  doDelete() {
    let pathPlusParam = this.package + '/' + this.assetName + '?stagingUser=' + this.stagingCuid;
    let url = this.context.serviceRoot + '/Assets/' + pathPlusParam;
    $mdwUi.clearMessage();
    $mdwUi.hubLoading(true);
    let ok = false;
    fetch(new Request(url, {
      method: 'DELETE',
      headers: { Accept: 'application/json', 'mdw-app-id': 'mdw-hub' },
      credentials: 'same-origin'
    }))
    .then(response => {
      $mdwUi.hubLoading(false);
      ok = response.ok;
      return response.json();
    })
    .then(json => {
      if (ok) {
        location = '../';
      }
      else {
        $mdwUi.showMessage(json.status.message);
      }
    });    
  }

  handleViewChange(view) {
    this.setState({
      view: view,
      asset: this.state.asset, 
      content: this.state.content
    });
  }

  handleDrop(files) {
    if (files && files.length === 1) {
      $mdwUi.clearMessage();
      let ok = false;  
      const url = this.context.hubRoot + '/asset/' + this.package + '/' + this.assetName + '?stagingUser=' + this.stagingCuid;
      const reader = new FileReader();
      reader.onload = () => {
        $mdwUi.hubLoading(true);
        fetch(url, { 
          method: 'PUT',
          headers: { 'Content-Type': 'application/octet-stream', 'mdw-app-id': 'mdw-hub' },
          body: new Int8Array(reader.result),
          credentials: 'same-origin'
        })
        .then(response => {
          $mdwUi.hubLoading(false);
          ok = response.ok;
          return response.json();
        })
        .then(json => {
          if (ok) {
            location = '../';
          }
          else {
            $mdwUi.showMessage(json.status.message);
          }
        });
      };
      reader.readAsArrayBuffer(files[0]);      
    }
  }

  handleFileOpen(event) {
    this.dropzone.current.open();
    event.preventDefault();
  }

  getRenderedMarkdown() {
    return { __html: this.state.content };
  }

  render() {
    let extension = undefined;
    let language = undefined;
    if (this.state.asset) {
      extension = this.state.asset.name.substring(this.state.asset.name.lastIndexOf('.') + 1);
      language = languages.getLanguage(extension);
    }

    return (
      <div>
        <AssetHeader 
          stagingCuid={this.stagingCuid}
          package={this.package} 
          assetName={this.assetName} 
          asset={this.state.asset} 
          view={this.state.view}
          onUnstage={this.handleUnstage}
          onDelete={this.handleDelete}
          onViewChange={this.handleViewChange} />
        <div className="mdw-section">
          {this.state.asset && (this.state.asset.isImage || this.state.view === 'upload' || !this.state.asset.isBinary) && 
            <div>
              {this.state.view === 'asset' &&
                <div>
                  {extension === 'proc' && this.state.content &&
                    <Workflow 
                      process={JSON.parse(this.state.content)} 
                      hubBase={this.context.hubRoot} 
                      serviceBase={this.context.serviceRoot} />                  
                  }
                  {extension === 'md' && this.state.content &&
                    <div className="mdw-item-content" dangerouslySetInnerHTML={this.getRenderedMarkdown()}></div>
                  }
                  {this.state.asset.isImage &&
                    <img src={this.context.hubRoot + '/asset/' + this.package + '/' + this.assetName + '?stagingUser=' + this.stagingCuid}
                      alt={this.state.asset.name} />
                  }
                  {extension !== 'proc' && extension !== 'md' && !this.state.asset.isBinary && this.state.content &&
                    <div>
                      <CodeBlock 
                        language={language} 
                        code={this.state.content} 
                        lineNumbers={true} />
                    </div>
                  }
                </div>
              }
              {this.state.view === 'upload' &&
                <div>
                  <Dropzone className="mdw-dropzone"
                    onDrop={this.handleDrop} disableClick={true} ref={this.dropzone} >
                    <div className="mdw-attach-zone">
                      Drag and drop new asset version, or {' '} 
                      <a href="" onClick={this.handleFileOpen}>
                        select files
                      </a>.
                    </div>
                  </Dropzone>
                  <div style={{textAlign:'right',marginTop:'6px'}}>
                    <Button className="mdw-btn"
                      onClick={() => this.handleViewChange('asset') }>
                      Cancel
                    </Button>
                  </div>
                </div>
              }
              {this.state.view === 'diff' &&
                <CodeDiff 
                  language={language} 
                  newLabel="Staged"
                  oldLabel="Original"
                  newContent={this.state.newContent}
                  oldContent={this.state.oldContent} />
              }
            </div>
          }
        </div>
        <Confirm title="Unstage Asset"
            ref={this.confirmUnstageDialog} 
            onClose={this.handleConfirmUnstage} />
        <Confirm title="Delete Asset" 
            ref={this.confirmDeleteDialog} 
            onClose={this.handleConfirmDelete} />
      </div>
    );
  }
}

StagedAsset.contextType = MdwContext;
export default StagedAsset; 
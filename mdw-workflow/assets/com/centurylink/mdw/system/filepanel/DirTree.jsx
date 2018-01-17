import React from '../../node/node_modules/react';
import PropTypes from '../../node/node_modules/prop-types';
import TreeView from '../../node/node_modules/react-treeview';
import '../../node/node_modules/style-loader!../../react/react-treeview.css';

function DirTree(props, context) {

  const handleClick = event => {
      if (props.onSelect) {
        props.onSelect()
      }
  };
  
  const dirLabel = <span className="fp-item">{props.dir.path}</span>;
  return (
    <TreeView 
      key={props.dir.path} 
      nodeLabel={dirLabel}
      defaultCollapsed={false}>
      {props.dir.dirs &&
        props.dir.dirs.map(dir => {
          return (
            <DirTree 
              key={dir.path}
              className="fp-item"
              dir={dir} 
              onSelect={props.onSelect} 
              onClick={event => props.onSelect(dir)} />
          );
        })
      }
      {props.dir.files &&
        props.dir.files.map(file => {
          return (
            <div 
              key={file.path} 
              onClick={event => props.onSelect(file)}>
              <span className="fp-item">{file.path}</span>
            </div>
          );
        })
      }
    </TreeView>
  );
}
  
DirTree.contextTypes = {
  hubRoot: PropTypes.string,
  serviceRoot: PropTypes.string
};

export default DirTree;
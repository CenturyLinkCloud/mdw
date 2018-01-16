import React from '../../node/node_modules/react';
import PropTypes from '../../node/node_modules/prop-types';
import TreeView from '../../node/node_modules/react-treeview';
import '../../node/node_modules/style-loader!../../react/react-treeview.css';

function DirTree(props, context) {
    
  return (
    <TreeView key={props.dir.path} nodeLabel={props.dir.path} defaultCollapsed={false}>
      {props.dir.dirs &&
        props.dir.dirs.map(dir => {
          return (
            <DirTree key={dir.path} dir={dir} />
          );
        })
      }
      {props.dir.files &&
        props.dir.files.map(file => {
          return (
            <div key={file.path} className="info">{file.path}</div>
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
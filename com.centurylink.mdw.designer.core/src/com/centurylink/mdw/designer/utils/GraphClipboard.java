/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.designer.utils;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import com.centurylink.mdw.designer.display.GraphFragment;
import com.centurylink.mdw.designer.display.Node;
import com.centurylink.mdw.designer.display.SubGraph;

public class GraphClipboard implements ClipboardOwner {

	private Clipboard clipboard;
	private static DataFlavor graphFragmentFlavor;
	private static GraphClipboard singleton = null;

	private GraphClipboard() {
		clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
	}

	public void put(Object obj) {
		ProcessTransferVO transfer;
		GraphFragment fragment;
		if (obj instanceof Node) {
			fragment = new GraphFragment(((Node)obj).graph.getProcessVO().getProcessId());
			fragment.nodes.add((Node)obj);
		} else if (obj instanceof SubGraph) {
			fragment = new GraphFragment(((SubGraph)obj).getGraph().getId());
			fragment.subgraphs.add((SubGraph)obj);
		} else if (obj instanceof GraphFragment) {
			fragment = (GraphFragment)obj;
		} else return;
		transfer = new ProcessTransferVO(fragment);
		clipboard.setContents(transfer, this);
	}

	public GraphFragment get() {
		Transferable content = clipboard.getContents(this);
		if (content!=null) {
			try {
				Object obj = content.getTransferData(graphFragmentFlavor);
				if (obj instanceof GraphFragment) return (GraphFragment)obj;
				else return null;
			} catch (Exception e) {
				// System.err.println("The clipboard content is not a graph fragment");
				return null;
			}
		} else return null;
	}

	public void lostOwnership(Clipboard clipboard, Transferable contents) {
		// do nothing - empty ClipboardOwner implementation
	}

	static class ProcessTransferVO implements Transferable {

		private GraphFragment procfragment;

		ProcessTransferVO(GraphFragment fragment) {
			procfragment = fragment;
		}

		public Object getTransferData(DataFlavor flavor)
			throws UnsupportedFlavorException, IOException {
			if (flavor.equals(graphFragmentFlavor)) {
				return procfragment;
			} else if (flavor.equals(DataFlavor.stringFlavor)) {
				return procfragment.toString();
			} else throw new UnsupportedFlavorException(flavor);
		}

		public DataFlavor[] getTransferDataFlavors() {
			DataFlavor[] flavors = new DataFlavor[2];
			flavors[0] = graphFragmentFlavor;
			flavors[1] = DataFlavor.stringFlavor;
			return flavors;
		}

		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return flavor==graphFragmentFlavor;
		}
	}

	public static GraphClipboard getInstance() {
		if (singleton==null) {
			graphFragmentFlavor = new DataFlavor(GraphFragment.class, "GraphFragment");
			singleton = new GraphClipboard();
		}
		return singleton;
	}

}

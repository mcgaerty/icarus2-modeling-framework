/**
 *
 */
package de.ims.icarus2.model.standard.members.structure;

import static de.ims.icarus2.model.api.ModelTestUtils.assertModelException;
import static de.ims.icarus2.model.api.ModelTestUtils.mockEdge;
import static de.ims.icarus2.test.TestUtils.random;
import static de.ims.icarus2.util.collections.CollectionUtils.list;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import de.ims.icarus2.model.api.ModelErrorCode;
import de.ims.icarus2.model.api.members.item.Edge;

/**
 * @author Markus Gärtner
 *
 */
class NodeTests {

	static final int MIN = 3;
	static final int MAX = 10;

	static int randomCount() {
		return random(MIN, MAX);
	}

	static int fillRandom(NodeInfo info, boolean incoming) {
		int count = randomCount();
		fill(info, count, incoming);
		return count;
	}

	static void fill(NodeInfo info, int count, boolean incoming) {
		while (count-- > 0) {
			info.addEdge(mockEdge(), incoming);
		}
	}

	static Edge[] randomEdges() {
		int count = randomCount();
		return edges(count);
	}

	static Edge[] edges(int count) {
		Edge[] edges = new Edge[count];
		for (int i = 0; i < edges.length; i++) {
			edges[i] = mockEdge();
		}
		return edges;
	}

	static void fill(NodeInfo info, Edge[] edges, boolean incoming) {
		for (Edge edge : edges) {
			info.addEdge(edge, incoming);
		}
	}

	static void assertEdgeAt(NodeInfo info, Edge[] edges, boolean incoming) {
		for (int i = 0; i < edges.length; i++) {
			assertEquals(edges[i], info.edgeAt(i, incoming));
		}

		for(int index : new int[]{-1, edges.length, edges.length+1}) {
			assertModelException(ModelErrorCode.MODEL_INDEX_OUT_OF_BOUNDS,
					() -> info.edgeAt(index, incoming));
		}
	}


	static void fillAndAssert(NodeInfo info, Edge[] edges, boolean incoming) {
		fill(info, edges, incoming);
		assertEdgeAt(info, edges, incoming);
	}

	static void fillAndAssert(NodeInfo info, int count, boolean incoming) {
		Edge[] edges = count==-1 ? randomEdges() : edges(count);
		fill(info, edges, incoming);
		assertEdgeAt(info, edges, incoming);
	}

	static void fillAndAssert(NodeInfo info, int countIn, int countOut) {
		Edge[] edgesIn = countIn==-1 ? randomEdges() : edges(countIn);
		Edge[] edgesOut = countOut==-1 ? randomEdges() : edges(countOut);
		fill(info, edgesIn, true);
		fill(info, edgesOut, false);
		assertEdgeAt(info, edgesIn, true);
		assertEdgeAt(info, edgesOut, false);
	}

	static void assertEdgeAt(NodeInfo info, List<Edge> edges, boolean incoming) {
		for (int i = 0; i < edges.size(); i++) {
			assertEquals(edges.get(i), info.edgeAt(i, incoming));
		}
	}

	static void removeAndAssert(NodeInfo info, Edge[] edges, boolean incoming) {
		List<Edge> buffer = list(edges);

		while(!buffer.isEmpty()) {
			int index = random(0, buffer.size());
			Edge edge = buffer.remove(index);
			info.removeEdge(edge, incoming);

			assertEdgeAt(info, buffer, incoming);

			// Make sure repeated attempts to remove fail
			assertModelException(ModelErrorCode.MODEL_ILLEGAL_MEMBER,
					() -> info.removeEdge(edge, incoming));
		}
	}

	static void fillRemoveAndAssert(NodeInfo info, int count, boolean incoming) {
		Edge[] edges = count==-1 ? randomEdges() : edges(count);
		fill(info, edges, incoming);
		removeAndAssert(info, edges, incoming);
	}

	static void fillRemoveAndAssert(NodeInfo info, int countIn, int countOut) {
		Edge[] edgesIn = countIn==-1 ? randomEdges() : edges(countIn);
		Edge[] edgesOut = countOut==-1 ? randomEdges() : edges(countOut);
		fill(info, edgesIn, true);
		fill(info, edgesOut, false);

		removeAndAssert(info, edgesIn, true);
		removeAndAssert(info, edgesOut, false);

		assertModelException(ModelErrorCode.MODEL_ILLEGAL_MEMBER,
				() -> info.removeEdge(mockEdge(), true));
		assertModelException(ModelErrorCode.MODEL_ILLEGAL_MEMBER,
				() -> info.removeEdge(mockEdge(), false));
	}
}
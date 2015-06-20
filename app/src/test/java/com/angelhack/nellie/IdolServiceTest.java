package com.angelhack.nellie;

import com.angelhack.nellie.api.IdolHelper;
import com.angelhack.nellie.model.Face;
import java.util.List;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Created by wimagguc on 20/06/15.
 */

public class IdolServiceTest {

    @Test
    public void testTest() {

        List<Face> faces = IdolHelper.listFaces();

        for (int i=0; i<faces.size(); i++) {
            System.out.println("face top=" + faces.get(i).getTop() );
        }

        assertFalse(false);
    }

}



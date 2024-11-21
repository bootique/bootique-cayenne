package io.bootique.cayenne.v50.junit5.persistence.auto;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.exp.property.NumericProperty;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.exp.property.SelfProperty;

import io.bootique.cayenne.v50.junit5.persistence.Table1;

/**
 * Class _Table1 was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _Table1 extends PersistentObject {

    private static final long serialVersionUID = 1L;

    public static final SelfProperty<Table1> SELF = PropertyFactory.createSelf(Table1.class);

    public static final String ID_PK_COLUMN = "id";

    public static final NumericProperty<Long> A = PropertyFactory.createNumeric("a", Long.class);
    public static final NumericProperty<Long> B = PropertyFactory.createNumeric("b", Long.class);

    protected Long a;
    protected Long b;


    public void setA(Long a) {
        beforePropertyWrite("a", this.a, a);
        this.a = a;
    }

    public Long getA() {
        beforePropertyRead("a");
        return this.a;
    }

    public void setB(Long b) {
        beforePropertyWrite("b", this.b, b);
        this.b = b;
    }

    public Long getB() {
        beforePropertyRead("b");
        return this.b;
    }

    @Override
    public Object readPropertyDirectly(String propName) {
        if(propName == null) {
            throw new IllegalArgumentException();
        }

        switch(propName) {
            case "a":
                return this.a;
            case "b":
                return this.b;
            default:
                return super.readPropertyDirectly(propName);
        }
    }

    @Override
    public void writePropertyDirectly(String propName, Object val) {
        if(propName == null) {
            throw new IllegalArgumentException();
        }

        switch (propName) {
            case "a":
                this.a = (Long)val;
                break;
            case "b":
                this.b = (Long)val;
                break;
            default:
                super.writePropertyDirectly(propName, val);
        }
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        writeSerialized(out);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        readSerialized(in);
    }

    @Override
    protected void writeState(ObjectOutputStream out) throws IOException {
        super.writeState(out);
        out.writeObject(this.a);
        out.writeObject(this.b);
    }

    @Override
    protected void readState(ObjectInputStream in) throws IOException, ClassNotFoundException {
        super.readState(in);
        this.a = (Long)in.readObject();
        this.b = (Long)in.readObject();
    }

}
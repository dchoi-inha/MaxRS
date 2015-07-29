package maxRA.record;


/**
 * @author Dongwan Choi
 * @date 2011. 12. 1.
 * 
 * This class implements a rectangle.
 */
public class Circle {
	public static final int SIZE = Point.SIZE + 4;
//--------on disk--------
	private Point p; // center point
	private double r; // radius
//-----------------------
	
	private int weight;
    // The boundary points of a circle
    public Point bp[];
    
	// Construct a circle without any specification
	public Circle()
    {
		p = new Point(0,0);
		r = 0;
    }
	
	public Circle( Point center, int radius ) {
		this.p = center;
		this.r = radius;
	}
	
	public Circle( Point center, int radius, int weight ) {
		this.p = center;
		this.r = radius;
		this.weight = weight;
	}
	
    // Construct a circle based on one point
	public Circle(Point center)
    {
		p = new Point(center.getX(), center.getY());
		r = 0;
    }
    // Construct a circle based on two points
    public Circle(Point p1, Point p2)
    {
		p = p1.midPoint(p2);
		r = p1.distance(p);
		bp = new Point[2];
		bp[0] = p1;
		bp[1] = p2;
    }
	
    // Construct a circle based on three points
    public Circle(Point p1, Point p2, Point p3)
    {
    	double x = (p3.getX()*p3.getX() * (p1.getY()-p2.getY()) + (p1.getX()*p1.getX() + (p1.getY()-p2.getY())*(p1.getY()-p3.getY())) 
    			* (p2.getY()-p3.getY()) + p2.getX()*p2.getX() * (-p1.getY()+p3.getY())) 
    			/ (2 * (p3.getX() * (p1.getY()-p2.getY()) + p1.getX() * (p2.getY()-p3.getY()) + p2.getX() * (-p1.getY()+p3.getY())));

    	double y = 0;
    	if ( p2.getY() != p3.getY() )
    		y = (p2.getY()+p3.getY())/2 - (p3.getX() - p2.getX())/(p3.getY()-p2.getY()) * (x - (p2.getX() + p3.getX())/2);
    	else if ( p3.getY() != p1.getY()) 
    		y = (p1.getY()+p3.getY())/2 - (p3.getX() - p1.getX())/(p3.getY()-p1.getY()) * (x - (p1.getX() + p3.getX())/2);
    	else
    		y = Double.NaN;
    	p = new Point(x, y);
    	r = p.distance(p1);
    	bp = new Point[3];
    	bp[0] = p1;	bp[1] = p2;	bp[2] = p3;

    	// DEBUG
    	if ( Double.isNaN(r) )
    		System.out.println(p.toString() + p1.toString() + p2.toString() + p3.toString());
    	// DEBUG
    	
    }
    
    public Point getCenter() {
    	return p;
    }
    
    public void setRadius(double r) {
    	this.r = r;
    }
    
    public double getRadius() {
    	return r;
    }

	public String toString() {
		return p.toString() + "{r=" +r+"} with count="+weight;
	}
	
	public int getWeight() {
		return weight;
	}
	
	public void setWeight( int weight ) {
		this.weight = weight;
	}
	
	public void incrementWeight() {
		this.weight++;
	}
	
	public boolean contain(Point p1)
	{
		return ( p.distance(p1) < r );
	}
	
	// Is a point in the circle
	public int checkContainment(Point point)
	{
		int answer = 0;
		double d = p.distance(point);
		if (d > r)
		{
			answer = 1;		// The point is outside the circle
		}
		else if (d == r)
		{
			answer = 0;		// The point is on the circumference of the circle
		}
		else
		{
			answer = -1;	// The point is inside the circle
		}
		return answer;
	}
}

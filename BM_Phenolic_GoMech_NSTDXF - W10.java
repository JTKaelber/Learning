

import Weng.System.*;
import Weng.Geometry.*;
import Weng.Modeler.*;
import Weng.CodeGen.MiscAddr;
import java.util.Vector;
import java.io.*;


public class BM_Phenolic_GoMech_NSTDXF implements WengMacro
{
	private boolean DEBUG = true;
	private boolean ACDB  = false;
	private final double TEXT_HEIGHT = 1;  // 1 Inch For Create Part Label

	public void main()
	{
		try
		{
			// Enable output to the EWM.
			Msg.DiagnosticsEnable( DEBUG );

			Init();

			if (DEBUG) Msg.Diagnostic("passed Init()");

			LayersDump();

			if (DEBUG) Msg.Diagnostic("passed LayersDump()");

			ModelDump();

			Terminate();

			Msg.Diagnostic("Done.");
		}
		catch (Exception e)
		{
			Msg.Diagnostic("Failed to create <" + m_path + ">");
			ExceptionPrinter.StackTracePrint( e );
		}
	}

	private void LayersDump() throws IOException
	{
  		DbTool	dbTool;
  		String	layers, toolname;
		int		count, indx;


		StringDump( "  0" );
		StringDump( "SECTION" );

		StringDump( "  2" );
		StringDump( "TABLES" );

		StringDump( "  0" );
		StringDump( "TABLE" );

		StringDump( "  2" );
		StringDump( "LAYER" );

		if ( ACDB )
		{
			StringDump( "100" );
			StringDump( "AcDbSymbolTable" );
		}

		count = Model.EntityCount( Const.TOOL );
		for (indx = 0; indx < count; ++indx)
		{
			dbTool = Model.ToolGet( indx );


			if (dbTool.RefCount() > 0)
			{
				if ( ACDB )
				{
					StringDump( "100" );
					StringDump( "AcDbLayerTableRecord" );
				}

				StringDump( "  0" );
				StringDump( "LAYER" );

				StringDump( "  2" );
				StringDump( dbTool.Name() );

				StringDump( " 70" );
				StringDump( "  0" );

				StringDump( " 62" );
				StringDump( "  7" );

				StringDump( "  6" );
				StringDump( "CONTINUOUS" );
			}
  		}

		StringDump( "  0" );
		StringDump( "ENDTAB" );

		StringDump( "  0" );
		StringDump( "ENDSEC" );
	}

	private void ModelDump() throws IOException
	{

		StringDump( "  0" );
		StringDump( "SECTION" );

		StringDump( "  2" );
		StringDump( "ENTITIES" );

		// CRITICAL: The following statement flattens the model,
		// providing access to the geometry without having to
		// traverse the contents of any instances.
		Portal.Execute("Pattern:Explode:all=1");

		InstanceTextDump();

		RemovePatterns();

		MaterialOutput();

		LinesDump();

		if (DEBUG) Msg.Diagnostic("passed LinesDump()");

		ArcsDump();

		if (DEBUG) Msg.Diagnostic("passed ArcsDump()");

		//We are now just outputting Layer geoemetry so we dont need to look for holes
		//HolesDump();

		//if (DEBUG) Msg.Diagnostic("passed HolesDump()");

		StringDump( "  0" );
		StringDump( "ENDSEC" );
	}

	private void RemovePatterns() throws IOException
	{
			Selector.Restrictions(true);
			Selector.All(false);
			Selector.Pattern(true);
			Selector.AddAll();

			Portal.Execute("Selector:SelectAll: chs=0");

			if (DEBUG) Msg.Diagnostic( "selector count:" + Selector.Count() );

			int count = Selector.Count();

			for (int indx = 0; indx < count; ++indx)
			{
				Selector.Get( indx ).Delete();
			}
	}
	private void MatLineDump( String layerName, double ps_x, double ps_y, double pe_x, double pe_y ) throws IOException
	{

		StringDump( "  0" );
		StringDump( "LINE" );

		if ( ACDB )
		{
			StringDump( "100" );
			StringDump( "AcDbLine" );
		}

		StringDump( "  8" );
		StringDump( layerName );

		StringDump( " 10" );
		DoubleDump( ps_x );

		StringDump( " 20" );
		DoubleDump( ps_y );

		StringDump( " 30" );
		StringDump( "0" );

		StringDump( " 11" );
		DoubleDump( pe_x );

		StringDump( " 21" );
		DoubleDump( pe_y );

		StringDump( " 31" );
		StringDump( "0" );
	}

	private void MaterialOutput() throws IOException
	{
		double dx = Model.DoubleGet("Width") * -1;
		double dy = Model.DoubleGet("Length");
		int workzone = Model.IntGet("WorkplaneType");

		if(workzone == 2)
		{
			dy = dy * -1;
		}

		MatLineDump( "Stock",0., 0., 0., -dy );
		MatLineDump( "Stock",0., -dy, -dx, -dy );
		MatLineDump( "Stock",-dx, -dy, -dx, 0. );
		MatLineDump( "Stock",-dx, 0., 0., 0. );
	}

	private void InstanceTextDump() throws IOException
	{
		DbTool	dbTool;
		String cutbacktext;
		int jindx;

		int count = Model.EntityCount( Const.COMMAND );

		// JTK - Vector for holding Y Points
		Vector<Double> ptsX = new Vector<>();
		double tempX;
		double newX;

		for (int indx = 0; indx < count; ++indx )
		{
			if (DEBUG) Msg.Diagnostic (" The indx is " + indx);

			DbCommand dbCommand = Model.CommandGet( indx );

			// NOTE: Correct placement of the instance text may
			// require that you add the sheet width to the Y-ordinate.
			Point pt = dbCommand.Point();

			// JTK - Establish this point as a temporary Double for
			// comparisons and adjustments
			tempX = pt.X();
			// JTK - Establish this pt into the Vector
			ptsX.add(tempX);

			String text = dbCommand.Text();

			jindx = text.lastIndexOf("Cutback");

			// JTK - Step through all Y Points, check if its touching another Y-Coordinate in ptsY array
			// if so, move it up or down farther by TEXT_HEIGHT to clear it
			for (int count2 = 0; count2 < ptsX.size(); count2++)
			{
				if (indx == count2)
					tempX = 0;
				else
				{
				
					if((tempX < (ptsX.get(count2) + TEXT_HEIGHT)) && (tempX > (ptsX.get(count2) - TEXT_HEIGHT)))
					{ 
						if(tempX >= ptsX.get(count2))
						{
							newX = tempX + (TEXT_HEIGHT * 5);
							pt.X(newX);
							ptsX.set(count2, pt.X());
						}
						if(tempX <= ptsX.get(count2))
						{
							newX = tempX - (TEXT_HEIGHT * 5);
							pt.X(newX);
							ptsX.set(count2, pt.X());
						}
					}
					else if(pt.X() == ptsX.get(count2))
					{	
						newX = tempX - (TEXT_HEIGHT * 5);
						pt.X(newX);
					}
				}
			}

			if (DEBUG) Msg.Diagnostic (" The jindx is " + jindx);

			//Note: do not output the legend because it is a multi line
			//text command

			if(jindx > 0)
				continue; //skip the command

			StringDump( "0" );
			StringDump( "TEXT" );
			StringDump( "  8" );
			StringDump( "0" );
			StringDump( " 62" );
			StringDump( "256" );
			StringDump( "  10" );
			DoubleDump( pt.Y() );
			StringDump( "  20" );
			DoubleDump( -pt.X() );
			StringDump( "  30" );
			StringDump( "0.0" );
			StringDump( "  40" );
			DoubleDump( 3.0 );
			StringDump( "  1" );

			StringDump( text );
			StringDump( "  50" );
			StringDump( "0.0" );
			StringDump( "  41" );
			StringDump( "1" );
			StringDump( "  7" );
			StringDump( "FONT2" );
		}
	}

	private void LinesDump() throws IOException
	{
		int count = Model.EntityCount( Const.LINE );
		int jindx;
		DbTool	dbTool;

		for (int indx = 0; indx < count; ++indx)
		{
			DbLine dbLine = Model.LineGet( indx );
			dbTool = dbLine.Tool();

			if ( dbTool.IsLayer() )
			{
				LineDump( dbLine.Tool().Name(), dbLine.StartPt(), dbLine.EndPt() );
			}
			else
			{
				continue;  // avoid further processing
			}
		}
	}

	private void ArcsDump() throws IOException
	{
		double		incang;
		double		ang0, ang1;
		int			dir;
		boolean		circle;
		DbTool	dbTool;

		// fodder to initialize arc (minimize memory abuse)
		Arc geoArc = new Arc( 0., 1., 0., 1., 0., 0., 0., 0., 0., -1 );
		double [] angles = new double[2];

		int count = Model.EntityCount( Const.ARC );
		for (int indx = 0; indx < count; ++indx)
		{
			DbArc dbArc = Model.ArcGet( indx );
			dbTool = dbArc.Tool();

			if ( dbTool.IsLayer() )
			{


				Point ps = dbArc.StartPt();
				Point pe = dbArc.EndPt();
				Point pc = dbArc.CenterPt();

				geoArc.Assign( ps, pe, pc, dbArc.Dir() );

				incang = geoArc.IncludedAngle() * Const.RAD2DEG;
				circle = (Math.abs( incang - 360. ) < 1.e-2);

				StringDump( "  0" );
				StringDump( (circle ? "CIRCLE" : "ARC") );

				if ( ACDB )
				{
					StringDump( "100" );
					StringDump( (circle ? " AcDbCircle" : "AcDbArc") );
				}

				StringDump( "  8" );
				StringDump( dbArc.Tool().Name() );

				StringDump( " 10" );
				DoubleDump( pc.Y());

				StringDump( " 20" );
				DoubleDump( -pc.X() );

				StringDump( " 30" );
				DoubleDump( pc.Z() );

				StringDump( " 40" );
				DoubleDump( dbArc.Radius() );

	  			if (circle == false)
	  			{
					geoArc.Angles( angles );

					ang0 = (angles[0] * Const.RAD2DEG) - 90.;
					ang1 = (angles[1] * Const.RAD2DEG) - 90.;

					dir = geoArc.Dir();

					StringDump( " 50" );
					DoubleDump( ((dir > 0) ? ang0 : ang1) );

					StringDump( " 51" );
					DoubleDump( ((dir > 0) ? ang1 : ang0) );
				}
			}
			else
			{
				continue;
			}
		}
	}

	private void HolesDump() throws IOException
	{
		double	SIZE = 0.125;

		Point ps = new Point();
		Point pe = new Point();

		int count = Model.EntityCount( Const.HOLE );
		for (int indx = 0; indx < count; ++indx)
		{
			DbHole dbHole = Model.HoleGet( indx );
			//if ( IsChildOfPattern( dbHole ) )
			//	continue;  // skip this entity

			DbTool dbTool = dbHole.Tool();

			Point pc = dbHole.CenterPt();

			int toolType = dbTool.IntGet("Type_ID");

			boolean isRound = (toolType == Const.ROUND);
			double radius = (isRound ? (0.5 * dbTool.DoubleGet("Diameter")) : SIZE );

			StringDump( "  0" );
			StringDump( "CIRCLE" );

			if ( ACDB )
			{
				StringDump( "100" );
				StringDump( " AcDbCircle" );
			}

			StringDump( "  8" );
			StringDump( dbHole.Tool().Name() );

			StringDump( " 10" );
			DoubleDump( pc.X() );

			StringDump( " 20" );
			DoubleDump( pc.Y() * -1);

			StringDump( " 30" );
			DoubleDump( pc.Z() );

			StringDump( " 40" );
			DoubleDump( radius );

			if (isRound == false)
			{
				ps.Assign( pc.X() * -1, (pc.Y() - SIZE), pc.Z() );
				pe.Assign( pc.X() * -1, (pc.Y() + SIZE), pc.Z() );
				LineDump( "BOGUS", ps, pe );

				ps.Assign( (pc.X() - SIZE) * -1, pc.Y(), pc.Z() );
				pe.Assign( (pc.X() + SIZE) * -1, pc.Y(), pc.Z() );
				LineDump( "BOGUS", ps, pe );
			}
		}
	}

	private void LineDump( String layerName, Point ps, Point pe ) throws IOException
	{

		if (layerName.equalsIgnoreCase ("Stock"))
			return; // early exit

		StringDump( "  0" );
		StringDump( "LINE" );

		if ( ACDB )
		{
			StringDump( "100" );
			StringDump( "AcDbLine" );
		}

		StringDump( "  8" );
		StringDump( layerName );

		StringDump( " 10" );
		DoubleDump (ps.Y());

		StringDump( " 20" );
		DoubleDump(ps.X() * -1 );

		StringDump( " 30" );
		DoubleDump( ps.Z() );

		StringDump( " 11" );
		DoubleDump( pe.Y());

		StringDump( " 21" );
		DoubleDump( pe.X() * -1 );

		StringDump( " 31" );
		DoubleDump( pe.Z() );
	}

	/*
	private boolean IsChildOfPattern( DbEntity dbEntity )
	{
		DbEntity dbOwner = dbEntity.Owner();
		while (true)
		{
			if (dbOwner == null)
				break;

			if (dbOwner.Type() == Const.PATTERN)
				break;

			dbOwner = dbOwner.Owner();
		}

		return (dbOwner != null);
	}
	*/

	private void StringDump( String sval ) throws IOException
	{
		m_file.write( sval );
		m_file.write( "\n" );
	}

	private void DoubleDump( double dval ) throws IOException
	{
		m_file.write( m_fmt.Uncond( dval ) );
		m_file.write( "\n" );
	}

	private void Init() throws IOException
	{
		String	sval;
		int		ival;

		//m_path = VarSys.StrGet("File");
		m_path = Model.StringGet("mac.nestdxf").trim();

		m_fmt = new MiscAddr( "", 5, 1, 4, 1, 1, 4, "0." ) ;

		m_file = new FileWriter( m_path );
	}

	private void Terminate() throws IOException
	{
  		StringDump( "  0" );
		StringDump( "EOF" );

		m_file.flush();
		m_file.close();
	}

	private FileWriter	m_file;
	private String		m_path;
	private MiscAddr	m_fmt;
}

